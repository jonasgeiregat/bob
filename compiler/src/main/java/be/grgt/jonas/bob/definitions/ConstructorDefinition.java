package be.grgt.jonas.bob.definitions;

import java.util.List;

public class ConstructorDefinition {

    private List<ParameterDefinition> parameters;

    public ConstructorDefinition(List<ParameterDefinition> parameters) {
        this.parameters = parameters;
    }

    public List<ParameterDefinition> parameters() {
        return parameters;
    }
}
