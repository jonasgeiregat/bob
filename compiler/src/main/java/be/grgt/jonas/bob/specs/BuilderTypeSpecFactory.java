package be.grgt.jonas.bob.specs;

import be.grgt.jonas.bob.BobTheBuilder;
import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.definitions.*;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static be.grgt.jonas.bob.utils.Formatter.format;

public class BuilderTypeSpecFactory {

    private static final String VOWELS = "AÀÁÂÃÄÅĀĂĄǺȀȂẠẢẤẦẨẪẬẮẰẲẴẶḀÆǼEȄȆḔḖḘḚḜẸẺẼẾỀỂỄỆĒĔĖĘĚÈÉÊËIȈȊḬḮỈỊĨĪĬĮİÌÍÎÏĲOŒØǾȌȎṌṎṐṒỌỎỐỒỔỖỘỚỜỞỠỢŌÒÓŎŐÔÕÖUŨŪŬŮŰŲÙÚÛÜȔȖṲṴṶṸṺỤỦỨỪỬỮỰYẙỲỴỶỸŶŸÝ";

    private TypeDefinition sourceDef;
    private Buildable buildable;

    private BuilderTypeSpecFactory(TypeDefinition source, Buildable buildable) {
        this.sourceDef = source;
        this.buildable = buildable;
    }

    public static TypeSpec produce(TypeDefinition source, Buildable buildable) {
        return new BuilderTypeSpecFactory(source, buildable).typeSpec();
    }

    private TypeSpec typeSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(builderTypeName(sourceDef))
                .superclass(ParameterizedTypeName.get(ClassName.get(BobTheBuilder.class), className(sourceDef)))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        if (!sourceDef.genericParameters().isEmpty())
            builder.addTypeVariables(toTypeVariableNames(sourceDef));
        builder.addMethod(newInstance());
        builder.addMethod(constructor());
        builder.addMethods(setters());
        if (!sourceDef.genericParameters().isEmpty())
            builder.addMethod(of());
        return builder.build();
    }

    private String builderTypeName(TypeDefinition source) {
        return format("$typeName$suffix", source.typeName(), "Builder");
    }

    public static String builderPackage(TypeDefinition source, Buildable buildable) {
        if(!buildable.packageName().isEmpty())
            return buildable.packageName();
        else
            return String.format("%s.builder", source.packageName());
    }

    private TypeName classNameWithoutGenerics(TypeDefinition definition) {
        return ClassName.get(definition.packageName(), definition.fullTypeName());
    }

    private TypeName className(SimpleTypeDefinition definition) {
        return ClassName.get(definition.packageName(), definition.fullTypeName());
    }

    private List<TypeName> simpleClassNames(List<SimpleTypeDefinition> definitions) {
        List<TypeName> typeNames = new ArrayList<>();
        for (SimpleTypeDefinition definition : definitions)
            typeNames.add(ClassName.get(definition.packageName(), definition.fullTypeName()));
        return typeNames;
    }

    private TypeName className(TypeDefinition definition) {
        if (definition.genericParameters().isEmpty()) {
            if (definition.isNested())
                return ClassName.get(definition.packageName(), definition.nestedIn()).nestedClass(definition.typeName());
            else
                return ClassName.get(definition.packageName(), definition.fullTypeName());
        } else {
            List<TypeVariableName> genericParameters = toTypeVariableNames(definition);
            return ParameterizedTypeName.get(ClassName.get(definition.packageName(), definition.fullTypeName()),
                    genericParameters.toArray(new TypeName[genericParameters.size()]));
        }
    }

    private List<TypeName> classNames(List<TypeDefinition> definitions) {
        List<TypeName> typeNames = new ArrayList<>();
        for (TypeDefinition def : definitions)
            typeNames.add(className(def));
        return typeNames;
    }


    private List<TypeVariableName> toTypeVariableNames(TypeDefinition definition) {
        List<TypeVariableName> genericParameters = new ArrayList<>();
        for (GenericParameterDefinition parameterDefinition : definition.genericParameters())
            genericParameters.add(TypeVariableName.get(parameterDefinition.name(), simpleClassNames(parameterDefinition.bounds()).toArray(new TypeName[parameterDefinition.bounds().size()])));
        return genericParameters;
    }

    private TypeName builderType() {
        if (sourceDef.genericParameters().isEmpty())
            return ClassName.get(builderPackage(sourceDef, buildable), builderTypeName(sourceDef));
        List<TypeVariableName> typeVariableNames = toTypeVariableNames(sourceDef);
        return ParameterizedTypeName.get(ClassName.get(builderPackage(sourceDef, buildable), builderTypeName(sourceDef)), typeVariableNames.toArray(new TypeName[typeVariableNames.size()]));
    }

    private List<TypeVariableName> builderTypeGenerics() {
        List<TypeVariableName> typeVariableNames = new ArrayList<>();
        for (GenericParameterDefinition param : sourceDef.genericParameters()) {
            List<TypeVariableName> bounds = new ArrayList<>();
            for (SimpleTypeDefinition definition : param.bounds()) {
                bounds.add(TypeVariableName.get(definition.typeName()));
            }
            typeVariableNames.add(TypeVariableName.get(param.name(), bounds.toArray(new TypeName[bounds.size()])));
        }
        return typeVariableNames;
    }

    private String fieldName(String name) {
        if(buildable.prefix().isEmpty())
            return name;
        return format("$prefix$name", buildable.prefix(), name.substring(0,1).toUpperCase() + name.substring(1));
    }

    private List<MethodSpec> setters() {
        List<MethodSpec> setters = new ArrayList<>();
        for (FieldDefinition field : sourceDef.fields()) {
            if (!field.isFinal() && notExcluded(field)) {
                MethodSpec.Builder setter = MethodSpec.methodBuilder(fieldName(field.name()))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType())
                        .addParameter(TypeName.get(field.type()), field.name());
                if (field.isPrivate() || field.isProtected() && notWithinTheSamePackage()) {
                    setter
                            .addStatement("setField($S, $L)", field.name(), field.name())
                            .build();
                } else {
                    setter
                            .addStatement("instance.$L = $L", field.name(), field.name())
                            .build();
                }
                setters.add(setter
                        .addStatement("return this")
                        .build());
            }
        }
        return setters;
    }

    private boolean notExcluded(FieldDefinition field) {
        return !Arrays.asList(buildable.excludes()).contains(field.name());
    }

    private boolean notWithinTheSamePackage() {
        return !sourceDef.packageName().equals(buildable.packageName());
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()")
                .build();
    }

    private MethodSpec of() {
        CodeBlock.Builder body = CodeBlock.builder();
        body.addStatement("return new $L<>()", builderTypeName(sourceDef));
        MethodSpec.Builder of = MethodSpec.methodBuilder("of")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariables(builderTypeGenerics())
                .returns(builderType());
        for (ParameterDefinition parameter : sourceDef.genericParameters())
            of.addParameter(ParameterizedTypeName.get(ClassName.get("java.lang", "Class"), TypeVariableName.get(parameter.name())), String.format("%stype", parameter.name()));
        of.addCode(body.build());
        return of.build();
    }


    private MethodSpec newInstance() {
        MethodSpec.Builder newInstance = MethodSpec.methodBuilder("newInstance")
                .addModifiers(Modifier.PROTECTED)
                .returns(className(sourceDef));
        if (defaultConstructorPresent())
            newInstance.addStatement("return new $L()", className(sourceDef));
        else
            newInstance.addStatement("   $L instance;try {\n" +
                    "\tinstance = $L.class.newInstance();\n" +
                    "} catch (InstantiationException e) {\n" +
                    "\tthrow new RuntimeException();\n" +
                    "} catch (IllegalAccessException e) {\n" +
                    "\tthrow new RuntimeException();\n" +
                    "}return instance", className(sourceDef), classNameWithoutGenerics(sourceDef));
        return newInstance.build();
    }

    private boolean defaultConstructorPresent() {
        if (sourceDef.constructors().isEmpty())
            return true;
        else
            for (ConstructorDefinition constructor : sourceDef.constructors())
                if (constructor.parameters().isEmpty())
                    return true;
        return false;
    }

    private static boolean isVowel(char c) {
        return VOWELS.indexOf(Character.toUpperCase(c)) > 0;
    }

}
