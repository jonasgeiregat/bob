package com.github.jonasgeiregat.bob.specs;

import com.github.jonasgeiregat.bob.Buildable;
import com.github.jonasgeiregat.bob.definitions.ConstructorDefinition;
import com.github.jonasgeiregat.bob.definitions.FieldDefinition;
import com.github.jonasgeiregat.bob.definitions.ParameterDefinition;
import com.github.jonasgeiregat.bob.definitions.TypeDefinition;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Predicate;
import com.google.common.base.Optional;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.github.jonasgeiregat.bob.utils.Formatter.format;

public class FieldsInsideBuilderTypeSpecFactory extends BaseTypeSpecFactory {

    protected FieldsInsideBuilderTypeSpecFactory(TypeDefinition source, Buildable buildable) {
        super(source, buildable);
    }

    @Override
    protected MethodSpec newInstance() {
        Optional<ConstructorDefinition> constructor = constructorMatchingFinalFields();
        if (!constructor.isPresent())
            throw new IllegalStateException("Missing constructor for final fields");
        return MethodSpec.methodBuilder("newInstance")
                .addModifiers(Modifier.PROTECTED)
                .returns(className(source))
                .addStatement("return new $L(" + constructorArguments(constructor.get().parameters()) + ")", args(constructor.get().parameters()))
                .build();
    }

    private Object[] args(List<ParameterDefinition> constructorParams) {
        final List<String> finalFields = finalFields();
        List<String> parameters = Stream.of(constructorParams)
                .map(new Function<ParameterDefinition, String>() {
                    @Override
                    public String apply(ParameterDefinition p) {
                        return finalFields.contains(p.name()) ? p.name() : "null";
                    }
                })
                .toList();
        Object[] args = new Object[constructorParams.size() + 1];
        args[0] = className(source);
        for (int i = 1; i <= parameters.size(); i++) {
            args[i] = parameters.get(i - 1);
        }
        return args;
    }

    private List<String> finalFields() {
        return Stream.of(source.fields(new Predicate<FieldDefinition>() {
            @Override
            public boolean test(FieldDefinition fieldDefinition) {
                return fieldDefinition.isFinal();
            }
        })).map(new Function<FieldDefinition, String>() {
            @Override
            public String apply(FieldDefinition fieldDefinition) {
                return fieldDefinition.name();
            }
        })
                .collect(Collectors.<String>toList());
    }

    private String constructorArguments(List<ParameterDefinition> params) {
        return Stream.of(params)
                .map(new Function<ParameterDefinition, String>() {
                    @Override
                    public String apply(ParameterDefinition parameterDefinition) {
                        return "$L";
                    }
                })
                .collect(Collectors.<String>joining(","));
    }

    private Optional<ConstructorDefinition> constructorMatchingFinalFields() {
        List<FieldDefinition> finalFields = source.fields(new Predicate<FieldDefinition>() {
            @Override
            public boolean test(FieldDefinition fieldDefinition) {
                return fieldDefinition.isFinal();
            }
        });
        for (ConstructorDefinition constructor : source.constructors()) {
            for (FieldDefinition finalField : finalFields) {
                if (!constructor.parameters().contains(finalField.name()))
                    break;
            }
            return Optional.of(constructor);
        }
        return Optional.absent();
    }

    @Override
    protected List<MethodSpec> setters() {
        List<MethodSpec> setters = new ArrayList<>();
        for (FieldDefinition field : source.fields()) {
            if (notExcluded(field)) {
                MethodSpec.Builder setter = MethodSpec.methodBuilder(fieldName(field.name()))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(builderType())
                        .addParameter(TypeName.get(field.type()), field.name());
                setter
                        .addStatement("this.$L = $L", field.name(), field.name())
                        .build();
                setters.add(setter
                        .addStatement("return this")
                        .build());
            }
        }
        return setters;
    }

    @Override
    protected List<FieldSpec> fields() {
        return
                Stream.of(source.fields())
                        .map(new Function<FieldDefinition, FieldSpec>() {
                            @Override
                            public FieldSpec apply(FieldDefinition field) {
                                return FieldSpec.builder(TypeName.get(field.type()), field.name(), Modifier.PRIVATE)
                                        .build();
                            }
                        })
                        .collect(Collectors.<FieldSpec>toList());
    }

    @Override
    protected MethodSpec build() {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(className(source))
                .addStatement(format("$type result = newInstance()", className(source).toString()));
        Stream.of(source.fields())
                .filter(new Predicate<FieldDefinition>() {
                    @Override
                    public boolean test(FieldDefinition field) {
                        return !field.isFinal();
                    }
                })
                .forEach(new Consumer<FieldDefinition>() {
                    @Override
                    public void accept(FieldDefinition field) {
                        if (field.isPublic())
                            builder.addStatement(format("result.$fieldName = $fieldName", field.name(), field.name()));
                        else if (field.isPrivate())
                            builder.addStatement(format("setField(result, \"$fieldName\",  $fieldName)", field.name(), field.name()));
                        else if (field.isProtected() && notWithinTheSamePackage())
                            builder.addStatement(format("setField(result, \"$fieldName\",  $fieldName)", field.name(), field.name()));
                        else
                            builder.addStatement(format("result.$fieldName = $fieldName", field.name(), field.name()));

                    }
                });
        builder.addStatement("return result", className(source).toString());
        return builder.build();
    }

    public static TypeSpec produce(TypeDefinition source, Buildable buildable) {
        return new FieldsInsideBuilderTypeSpecFactory(source, buildable).typeSpec();
    }
}
