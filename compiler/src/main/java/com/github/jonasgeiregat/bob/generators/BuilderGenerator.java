package com.github.jonasgeiregat.bob.generators;

import com.github.jonasgeiregat.bob.Buildable;
import com.github.jonasgeiregat.bob.TypeWriter;
import com.github.jonasgeiregat.bob.definitions.TypeDefinition;
import com.github.jonasgeiregat.bob.specs.InstanceInsideBuilderTypeSpecFactory;
import com.github.jonasgeiregat.bob.specs.TypeSpecFactory;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;

public class BuilderGenerator {

    private final Filer filer;

    public BuilderGenerator(Filer filer) {
        this.filer = filer;
    }

    public void generate(TypeDefinition source, Buildable buildable) {
        TypeSpec typeSpec = TypeSpecFactory.produce(source, buildable);
        TypeWriter.write(filer, InstanceInsideBuilderTypeSpecFactory.builderPackage(source, buildable), typeSpec);
    }

}