package com.github.jonasgeiregat.bob.definitions;

public class MethodDefinition {
    private final String name;

    public MethodDefinition(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
