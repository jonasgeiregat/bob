package be.grgt.jonas.bob.specs;

import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.definitions.ConstructorDefinition;
import be.grgt.jonas.bob.definitions.FieldDefinition;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static be.grgt.jonas.bob.utils.Formatter.format;

public class InstanceInsideBuilderTypeSpecFactory extends BaseTypeSpecFactory {

    private InstanceInsideBuilderTypeSpecFactory(TypeDefinition source, Buildable buildable) {
        super(source, buildable);
    }

    public static TypeSpec produce(TypeDefinition source, Buildable buildable) {
        return new InstanceInsideBuilderTypeSpecFactory(source, buildable).typeSpec();
    }

    private TypeName classNameWithoutGenerics(TypeDefinition definition) {
        return ClassName.get(definition.packageName(), definition.fullTypeName());
    }

    @Override
    protected List<MethodSpec> setters() {
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

    @Override
    protected List<FieldSpec> fields() {
        return Collections.emptyList();
    }

    @Override
    protected MethodSpec get() {
        return MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .returns(className(sourceDef))
                .addStatement(format("$type result = instance;\n" +
                        "instance = newInstance();\n" +
                        "return result", className(sourceDef).toString()))
                .build();
    }

    @Override
    protected MethodSpec newInstance() {
        MethodSpec.Builder newInstance = MethodSpec.methodBuilder("newInstance")
                .addModifiers(Modifier.PROTECTED)
                .returns(className(sourceDef));
        if (defaultConstructorPresent())
            newInstance.addStatement("return new $L()", className(sourceDef));
        else
            newInstance.addStatement("   $L instance;try {\n" +
                    "\tinstance = ($L) $L.class.getConstructors()[0].newInstance((Object[])new java.lang.reflect.Array[]{null});\n" +
                    "} catch (InstantiationException e) {\n" +
                    "\tthrow new RuntimeException();\n" +
                    "} catch (IllegalAccessException e) {\n" +
                    "\tthrow new RuntimeException();\n" +
                    "} catch (java.lang.reflect.InvocationTargetException e) {\n" +
                    "\tthrow new RuntimeException();\n" +
                    "}return instance", className(sourceDef), className(sourceDef), classNameWithoutGenerics(sourceDef));
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
}
