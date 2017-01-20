package be.grgt.jonas.bob.generators;

import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.TypeWriter;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import be.grgt.jonas.bob.processor.BuildableProcessor;
import be.grgt.jonas.bob.specs.BuildATypeSpecFactory;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import java.util.List;

public class BobGenerator {

    private Filer filer;

    public BobGenerator(Filer filer) {
        this.filer = filer;
    }

    public void generate(List<BuildableProcessor.Tuple<TypeDefinition, Buildable>> builders) {
        TypeSpec typeSpec = BuildATypeSpecFactory.produce(builders);
        TypeWriter.write(filer, "com.github.jonasgeiregat.bob", typeSpec);
    }
}
