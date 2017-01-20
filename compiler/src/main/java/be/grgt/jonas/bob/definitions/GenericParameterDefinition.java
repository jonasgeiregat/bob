package be.grgt.jonas.bob.definitions;

import java.util.List;

public class GenericParameterDefinition extends ParameterDefinition {

    private final List<SimpleTypeDefinition> bounds;

    public GenericParameterDefinition(String name, List<SimpleTypeDefinition> bounds) {
        super(name);
        this.bounds = bounds;
    }

    public List<SimpleTypeDefinition> bounds() {
        return bounds;
    }

}