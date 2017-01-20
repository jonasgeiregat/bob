package be.grgt.jonas.bob.definitions;


public class ParameterDefinition {

    private String name;

    public ParameterDefinition(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    private ParameterDefinition() {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private ParameterDefinition instance = new ParameterDefinition();

        public Builder name(String name) {
            instance.name = name;
            return this;
        }

        public ParameterDefinition build() {
            ParameterDefinition result = instance;
            instance = null;
            return result;
        }
    }
}
