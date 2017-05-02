package be.grgt.jonas.bob.definitions;


import com.annimon.stream.Collector;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TypeDefinition extends SimpleTypeDefinition {

    private List<FieldDefinition> fields = new ArrayList<>();
    private List<ConstructorDefinition> constructors = new ArrayList<>();
    private List<GenericParameterDefinition> genericParameters = new ArrayList<>();
    private List<MethodDefinition> methods;

    private TypeDefinition(String typeName, String packageName, String enclosedIn, List<FieldDefinition> fields, List<ConstructorDefinition> constructors) {
        super(typeName, packageName);
        this.parent = enclosedIn;
        this.fields = fields;
        this.constructors = constructors;
    }

    public TypeDefinition() {
        super();
    }

    public List<FieldDefinition> fields() {
        return fields;
    }

    public List<FieldDefinition> fields(Predicate<FieldDefinition> predicate) {
        return Stream.of(fields())
                .filter(predicate)
                .collect(Collectors.<FieldDefinition>toList());
    }

    public List<MethodDefinition> methods(Predicate<MethodDefinition> predicate) {
        return Stream.of(methods)
                .filter(predicate)
                .toList();
    }

    public String nestedIn() {
        return parent;
    }

    public List<GenericParameterDefinition> genericParameters() {
        return new ArrayList<>(genericParameters);
    }

    public List<ConstructorDefinition> constructors() {
        return new ArrayList<>(constructors);
    }

    public String fullTypeName() {
        return (parent != null ? parent + "."  : "") + typeName;
    }

    public boolean isNested() {
        return parent != null;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private TypeDefinition instance = new TypeDefinition();

        public Builder typeName(String typeName) {
            instance.typeName = typeName;
            return this;
        }

        public Builder packageName(String packageName) {
            instance.packageName = packageName;
            return this;
        }

        public Builder methods(List<MethodDefinition> methods) {
            instance.methods = methods;
            return this;
        }

        public Builder enclosedIn(String enclosedIn) {
            instance.parent = enclosedIn;
            return this;
        }

        public Builder fields(List<FieldDefinition> fields) {
            instance.fields = fields;
            return this;
        }

        public Builder constructors(List<ConstructorDefinition> constructors) {
            instance.constructors = constructors;
            return this;
        }

        public Builder genericParameters(List<GenericParameterDefinition> generics) {
            instance.genericParameters = generics;
            return this;
        }

        public TypeDefinition build() {
            TypeDefinition result = instance;
            instance = null;
            return result;
        }
    }
}
