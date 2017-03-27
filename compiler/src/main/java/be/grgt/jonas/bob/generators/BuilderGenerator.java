package be.grgt.jonas.bob.generators;

import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.TypeWriter;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import be.grgt.jonas.bob.specs.InstanceInsideBuilderTypeSpecFactory;
import be.grgt.jonas.bob.specs.TypeSpecFactory;
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