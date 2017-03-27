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

abstract class BaseTypeSpecFactory {

    protected TypeDefinition sourceDef;
    protected Buildable buildable;

    protected BaseTypeSpecFactory(TypeDefinition source, Buildable buildable) {
        this.sourceDef = source;
        this.buildable = buildable;
    }

    protected String builderTypeName(TypeDefinition source) {
        return format("$typeName$suffix", source.typeName(), "Builder");
    }

    protected TypeSpec typeSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(builderTypeName(sourceDef))
                .superclass(ParameterizedTypeName.get(ClassName.get(BobTheBuilder.class), className(sourceDef)))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        if (!sourceDef.genericParameters().isEmpty())
            builder.addTypeVariables(toTypeVariableNames(sourceDef));
        builder.addMethod(newInstance());
        builder.addMethods(setters());
        builder.addFields(fields());
        builder.addMethod(get());
        builder.addMethod(constructor());
        if (!sourceDef.genericParameters().isEmpty())
            builder.addMethod(of());
        return builder.build();
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

    protected TypeName builderType() {
        if (sourceDef.genericParameters().isEmpty())
            return ClassName.get(builderPackage(sourceDef, buildable), builderTypeName(sourceDef));
        List<TypeVariableName> typeVariableNames = toTypeVariableNames(sourceDef);
        return ParameterizedTypeName.get(ClassName.get(builderPackage(sourceDef, buildable), builderTypeName(sourceDef)), typeVariableNames.toArray(new TypeName[typeVariableNames.size()]));
    }

    public static String builderPackage(TypeDefinition source, Buildable buildable) {
        if (!buildable.packageName().isEmpty())
            return buildable.packageName();
        else
            return String.format("%s.builder", source.packageName());
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


    protected TypeName className(TypeDefinition definition) {
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

    protected List<TypeVariableName> toTypeVariableNames(TypeDefinition definition) {
        List<TypeVariableName> genericParameters = new ArrayList<>();
        for (GenericParameterDefinition parameterDefinition : definition.genericParameters())
            genericParameters.add(TypeVariableName.get(parameterDefinition.name(), simpleClassNames(parameterDefinition.bounds()).toArray(new TypeName[parameterDefinition.bounds().size()])));
        return genericParameters;
    }

    private List<TypeName> simpleClassNames(List<SimpleTypeDefinition> definitions) {
        List<TypeName> typeNames = new ArrayList<>();
        for (SimpleTypeDefinition definition : definitions)
            typeNames.add(ClassName.get(definition.packageName(), definition.fullTypeName()));
        return typeNames;
    }

    private TypeName classNameWithoutGenerics(TypeDefinition definition) {
        return ClassName.get(definition.packageName(), definition.fullTypeName());
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

    protected String fieldName(String name) {
        if(buildable.prefix().isEmpty())
            return name;
        return format("$prefix$name", buildable.prefix(), name.substring(0,1).toUpperCase() + name.substring(1));
    }

    protected boolean notExcluded(FieldDefinition field) {
        return !Arrays.asList(buildable.excludes()).contains(field.name());
    }

    protected boolean notWithinTheSamePackage() {
        return !sourceDef.packageName().equals(buildable.packageName());
    }

    protected MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()")
                .build();
    }

    protected abstract MethodSpec newInstance();
    protected abstract List<MethodSpec> setters();
    protected abstract List<FieldSpec> fields();
    protected abstract MethodSpec get();
}
