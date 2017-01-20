package be.grgt.jonas.bob.generators;

import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.TypeWriter;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import be.grgt.jonas.bob.specs.BuilderTypeSpecFactory;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;

public class BuilderGenerator {

    private final Filer filer;

    public BuilderGenerator(Filer filer) {
        this.filer = filer;
    }

    public void generate(TypeDefinition source, Buildable buildable) {
        TypeSpec typeSpec = BuilderTypeSpecFactory.produce(source, buildable);
        TypeWriter.write(filer, BuilderTypeSpecFactory.builderPackage(source, buildable), typeSpec);
    }

}