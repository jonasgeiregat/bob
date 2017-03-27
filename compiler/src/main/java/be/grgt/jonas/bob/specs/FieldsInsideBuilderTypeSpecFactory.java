package be.grgt.jonas.bob.specs;

import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.definitions.ConstructorDefinition;
import be.grgt.jonas.bob.definitions.FieldDefinition;
import be.grgt.jonas.bob.definitions.ParameterDefinition;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import be.grgt.jonas.bob.utils.Formatter;
import com.annimon.stream.Collector;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Predicate;
import com.google.common.base.Optional;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static be.grgt.jonas.bob.utils.Formatter.format;

public class FieldsInsideBuilderTypeSpecFactory extends BaseTypeSpecFactory {

    protected FieldsInsideBuilderTypeSpecFactory(TypeDefinition source, Buildable buildable) {
        super(source, buildable);
    }

    @Override
    protected MethodSpec newInstance() {
        MethodSpec.Builder newInstance = MethodSpec.methodBuilder("newInstance")
                .addModifiers(Modifier.PROTECTED)
                .returns(className(sourceDef));
        Optional<ConstructorDefinition> constructor = constructorMatchingFinalFields();
        if (!constructor.isPresent())
            throw new IllegalStateException("Missing constructor for final fields");
        return MethodSpec.methodBuilder("newInstance")
                .addModifiers(Modifier.PROTECTED)
                .returns(className(sourceDef))
                .addStatement("return new $L(" + constructorArguments(constructor.get().parameters()) + ")", args())
                .build();
    }

    private Object[] args() {
        List<String> finalFields = Stream.of(sourceDef.fields(new Predicate<FieldDefinition>() {
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
        Object[] args = new Object[finalFields.size() + 1];
        args[0] = className(sourceDef);
        for (int i = 1; i <= finalFields.size(); i++) {
            args[i] = finalFields.get(i - 1);
        }
        return args;
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
        List<FieldDefinition> finalFields = sourceDef.fields(new Predicate<FieldDefinition>() {
            @Override
            public boolean test(FieldDefinition fieldDefinition) {
                return fieldDefinition.isFinal();
            }
        });
        for (ConstructorDefinition constructor : sourceDef.constructors()) {
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
        for (FieldDefinition field : sourceDef.fields()) {
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
                Stream.of(sourceDef.fields())
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
    protected MethodSpec get() {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .returns(className(sourceDef))
                .addStatement(format("$type result = newInstance()", className(sourceDef).toString()));
        Stream.of(sourceDef.fields())
                .filter(new Predicate<FieldDefinition>() {
                    @Override
                    public boolean test(FieldDefinition field) {
                        return !field.isFinal();
                    }
                })
                .forEach(new Consumer<FieldDefinition>() {
                    @Override
                    public void accept(FieldDefinition field) {
                        if(field.isPublic())
                            builder.addStatement(format("result.$fieldName = $fieldName", field.name(), field.name()));
                        else if(field.isPrivate())
                            builder.addStatement(format("setField(result, \"$fieldName\",  $fieldName)", field.name(), field.name()));
                        else if(field.isProtected() && notWithinTheSamePackage())
                            builder.addStatement(format("setField(result, \"$fieldName\",  $fieldName)", field.name(), field.name()));
                        else
                            builder.addStatement(format("result.$fieldName = $fieldName", field.name(), field.name()));

                    }
                });
        builder.addStatement("return result", className(sourceDef).toString());
        return builder.build();
    }

    public static TypeSpec produce(TypeDefinition source, Buildable buildable) {
        return new FieldsInsideBuilderTypeSpecFactory(source, buildable).typeSpec();
    }
}
