package be.grgt.jonas.bob.specs;

import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.definitions.ConstructorDefinition;
import be.grgt.jonas.bob.definitions.FieldDefinition;
import be.grgt.jonas.bob.definitions.ParameterDefinition;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import com.google.common.collect.Sets;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.junit.Test;
import org.mockito.Matchers;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import java.util.*;

import static be.grgt.jonas.bob.utils.Formatter.format;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BuilderTypeSpecFactoryTest {

    private static final String NO_INNER_CLASS = null;

    @Test
    public void produceTest_normalPackagedBasedClass() {
        List<FieldDefinition> fields = new ArrayList<>();
        PrimitiveType primitiveType = primitiveType();
        fields.add(new FieldDefinition("count", Sets.newHashSet(Modifier.PRIVATE), primitiveType));

        TypeDefinition source = builderDefinition("com.wine.bar", "Cheese", NO_INNER_CLASS, fields, Collections.<ConstructorDefinition>emptyList());
        Buildable buildable = newBuildable().get();
        TypeSpec typeSpec = InstanceInsideBuilderTypeSpecFactory.produce(source, buildable);

        assertThat(((ParameterizedTypeName) typeSpec.superclass).typeArguments.get(0).toString())
                .isEqualTo("com.wine.bar.Cheese");
        assertThat(typeSpec.name)
                .isEqualTo("CheeseBuilder");
    }


    @Test
    public void produceTest_nestedClass() {
        List<FieldDefinition> fields = new ArrayList<>();
        PrimitiveType primitiveType = primitiveType();
        fields.add(new FieldDefinition("count", Sets.newHashSet(Modifier.PRIVATE), primitiveType));

        TypeDefinition source = builderDefinition("com.wine.bar", "Cheese", "Cave.Cellar", fields, Collections.<ConstructorDefinition>emptyList());
        Buildable buildable = newBuildable().get();
        TypeSpec typeSpec = InstanceInsideBuilderTypeSpecFactory.produce(source, buildable);

        String expectedClass = "com.wine.bar.Cave.Cellar.Cheese";
        assertThat(((ParameterizedTypeName) typeSpec.superclass).typeArguments.get(0).toString())
                .isEqualTo(expectedClass);
        assertThat(typeSpec.name)
                .isEqualTo("CheeseBuilder");
        assertThat(newInstance(typeSpec).returnType.toString())
                .isEqualTo(expectedClass);
        assertThat(newInstance(typeSpec).code.toString())
                .isEqualTo("return new " + expectedClass + "();\n");
    }

    @Test
    public void produceTest_newInstance_missingDefaultConstructor() {
        List<FieldDefinition> fields = new ArrayList<>();

        List<ConstructorDefinition> constructors = Collections.singletonList(new ConstructorDefinition(Collections.singletonList(new ParameterDefinition("name"))));
        TypeDefinition source = builderDefinition("com.wine.bar", "Cheese", "Cave.Cellar", fields, constructors);

        Buildable buildable = newBuildable().get();
        TypeSpec typeSpec = InstanceInsideBuilderTypeSpecFactory.produce(source, buildable);

        String expectedClass = "com.wine.bar.Cave.Cellar.Cheese";
        assertThat(((ParameterizedTypeName) typeSpec.superclass).typeArguments.get(0).toString())
                .isEqualTo(expectedClass);
        assertThat(typeSpec.name)
                .isEqualTo("CheeseBuilder");
        assertThat(newInstance(typeSpec).returnType.toString())
                .isEqualTo(expectedClass);
        assertThat(newInstance(typeSpec).code.toString())
                .isEqualTo("   " + expectedClass + " instance;try {\n" +
                        "    \tinstance = (" + expectedClass + ") " + expectedClass + ".class.getConstructors()[0].newInstance((Object[])new java.lang.reflect.Array[]{null});\n" +
                        "    } catch (InstantiationException e) {\n" +
                        "    \tthrow new RuntimeException();\n" +
                        "    } catch (IllegalAccessException e) {\n" +
                        "    \tthrow new RuntimeException();\n" +
                        "    } catch (java.lang.reflect.InvocationTargetException e) {\n" +
                        "    \tthrow new RuntimeException();\n" +
                        "    }return instance;\n");
    }


    @Test
    public void produceTest_protectedFields_areSetUsingReflectionWhenNotInSamePackage() {
        List<FieldDefinition> fields = new ArrayList<>();
        TypeMirror type = mock(TypeMirror.class);
        when(type.accept(any(TypeVisitor.class), Matchers.any())).thenReturn(TypeName.get(String.class));
        fields.add(new FieldDefinition("age", Collections.singleton(Modifier.PROTECTED), type));

        List<ConstructorDefinition> constructors = Collections.singletonList(new ConstructorDefinition(Collections.singletonList(new ParameterDefinition("name"))));
        TypeDefinition source = builderDefinition("com.wine.bar", "Cheese", "Cave.Cellar", fields, constructors);
        Buildable buildable = newBuildable().get();
        TypeSpec typeSpec = InstanceInsideBuilderTypeSpecFactory.produce(source, buildable);

        MethodSpec age = filter(typeSpec.methodSpecs, "age");
        assertThat(age.code.toString()).isEqualTo("setField(\"age\", age);\nreturn this;\n");
    }

    @Test
    public void produceTest_protectedFieldsThatAreInTheSamePackage_areSetDirectly() {
        List<FieldDefinition> fields = new ArrayList<>();
        TypeMirror type = mock(TypeMirror.class);
        when(type.accept(any(TypeVisitor.class), Matchers.any())).thenReturn(TypeName.get(String.class));
        fields.add(new FieldDefinition("age", Collections.singleton(Modifier.PROTECTED), type));

        List<ConstructorDefinition> constructors = Collections.singletonList(new ConstructorDefinition(Collections.singletonList(new ParameterDefinition("name"))));
        TypeDefinition source = builderDefinition("com.wine.bar", "Cheese", null, fields, constructors);
        Buildable buildable = newBuildable()
                .withPackageName("com.wine.bar")
                .get();
        TypeSpec typeSpec = InstanceInsideBuilderTypeSpecFactory.produce(source, buildable);

        MethodSpec age = filter(typeSpec.methodSpecs, "age");
        assertThat(age.code.toString()).isEqualTo("instance.age = age;\nreturn this;\n");
    }

    private MethodSpec filter(List<MethodSpec> specs, String name) {
        Objects.requireNonNull(name);
        for(com.squareup.javapoet.MethodSpec spec: specs)
            if(name.equals(spec.name))
                return spec;
        throw new IllegalStateException(format("Unable to find method named %name", name));
    }


    @Test
    public void produceTest_prefix_appliedCorrectly() {
        List<FieldDefinition> fields = new ArrayList<>();
        TypeMirror type = mock(TypeMirror.class);
        when(type.accept(any(TypeVisitor.class), Matchers.any())).thenReturn(TypeName.get(String.class));
        fields.add(new FieldDefinition("age", Collections.singleton(Modifier.PUBLIC), type));

        List<ConstructorDefinition> constructors = Collections.singletonList(new ConstructorDefinition(Collections.singletonList(new ParameterDefinition("name"))));
        TypeDefinition source = builderDefinition("com.wine.bar", "Cheese", "Cave.Cellar", fields, constructors);
        Buildable buildable = newBuildable()
                .withPrefix("with")
                .get();
        TypeSpec typeSpec = InstanceInsideBuilderTypeSpecFactory.produce(source, buildable);

        boolean found = false;
        for(MethodSpec method: typeSpec.methodSpecs) {
            if(Objects.equals(method.name, "withAge")) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    public void produceTest_excludes_satisfied() {
        List<FieldDefinition> fields = new ArrayList<>();
        TypeMirror type = mock(TypeMirror.class);
        when(type.accept(any(TypeVisitor.class), Matchers.any())).thenReturn(TypeName.get(String.class));
        fields.add(new FieldDefinition("age", Collections.singleton(Modifier.PUBLIC), type));
        fields.add(new FieldDefinition("taste", Collections.singleton(Modifier.PRIVATE), type));
        fields.add(new FieldDefinition("location", Collections.singleton(Modifier.PUBLIC), type));

        List<ConstructorDefinition> constructors = Collections.singletonList(new ConstructorDefinition(Collections.singletonList(new ParameterDefinition("name"))));
        TypeDefinition source = builderDefinition("com.wine.bar", "Cheese", "Cave.Cellar", fields, constructors);
        Buildable buildable = newBuildable()
                .withPrefix("with")
                .withExcludes("age", "taste")
                .get();
        TypeSpec typeSpec = InstanceInsideBuilderTypeSpecFactory.produce(source, buildable);

        boolean ageFound = false;
        boolean locationFound = false;
        boolean withTaste = false;
        for(MethodSpec method: typeSpec.methodSpecs) {
            if(Objects.equals(method.name, "withAge")) {
                ageFound = true;
            }
            if(Objects.equals(method.name, "withLocation")) {
                locationFound = true;
            }
            if(Objects.equals(method.name, "withTaste")) {
                withTaste = true;
            }
        }
        assertThat(ageFound).isFalse();
        assertThat(withTaste).isFalse();
        assertThat(locationFound).isTrue();
    }

    private MethodSpec newInstance(TypeSpec typeSpec) {
        for (MethodSpec spec : typeSpec.methodSpecs)
            if ("newInstance".equals(spec.name))
                return spec;
        throw new IllegalStateException("Method newInstance not found");
    }

    private TypeDefinition builderDefinition(String packageName, String typeName, String enclosedIn, List<FieldDefinition> fields, List<ConstructorDefinition> constructors) {
        TypeDefinition definition = mock(TypeDefinition.class);
        when(definition.typeName()).thenReturn(typeName);
        when(definition.typeName()).thenReturn(typeName);
        when(definition.packageName()).thenReturn(packageName);
        when(definition.nestedIn()).thenReturn(enclosedIn);
        when(definition.fullTypeName()).thenReturn(enclosedIn == null ? typeName : enclosedIn + "." + typeName);
        when(definition.constructors()).thenReturn(constructors);
        when(definition.fields()).thenReturn(fields);

        return definition;
    }

    private PrimitiveType primitiveType() {
        PrimitiveType primitiveType = mock(PrimitiveType.class);
        when(primitiveType.accept(any(TypeVisitor.class), Matchers.any())).thenReturn(mock(TypeName.class));
        return primitiveType;
    }

    private static BuildableBuilder newBuildable() {
        return new BuildableBuilder();
    }

    private static class BuildableBuilder {
        private Buildable buildable;
        private String prefix = "";
        private String packageName = "";
        private List<String> excludes = new ArrayList<>();

        BuildableBuilder() {
            buildable = mock(Buildable.class);
        }

        BuildableBuilder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        BuildableBuilder withExcludes(String ... excludes) {
            this.excludes.addAll(Arrays.asList(excludes));
            return this;
        }

        BuildableBuilder withPackageName(String name) {
            this.packageName = name;
            return this;
        }

        Buildable get() {
            when(buildable.prefix()).thenReturn(prefix);
            when(buildable.packageName()).thenReturn(packageName);
            when(buildable.excludes()).thenReturn(excludes.toArray(new String[excludes.size()]));
            return buildable;
        }
    }

}