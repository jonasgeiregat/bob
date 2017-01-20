package be.grgt.jonas.bob.specs;

import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.definitions.GenericParameterDefinition;
import be.grgt.jonas.bob.definitions.SimpleTypeDefinition;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import be.grgt.jonas.bob.processor.BuildableProcessor;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

public class BuildATypeSpecFactory {

    private List<BuildableProcessor.Tuple<TypeDefinition, Buildable>> builders;

    public BuildATypeSpecFactory(List<BuildableProcessor.Tuple<TypeDefinition, Buildable>> builders) {
        this.builders = builders;
    }

    private TypeSpec typeSpec() {
        TypeSpec.Builder buildA = TypeSpec.classBuilder("Bob")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        for(BuildableProcessor.Tuple<TypeDefinition, Buildable> builder: builders) {
            addBuilderMethod(buildA, builder);
        }
        return buildA.build();
    }

    private void addBuilderMethod(TypeSpec.Builder buildA, BuildableProcessor.Tuple<TypeDefinition, Buildable> builder) {
        buildA.addMethod(MethodSpec.methodBuilder("buildA" + builder.definition.typeName())
                .returns(className(builder.definition))
                .build());
    }

    public static TypeSpec produce(List<BuildableProcessor.Tuple<TypeDefinition, Buildable>> builders) {
        return new BuildATypeSpecFactory(builders).typeSpec();
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

    public String typeName(String name) {
        return name + "Builder";
    }

    private List<TypeVariableName> toTypeVariableNames(TypeDefinition definition) {
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

}
