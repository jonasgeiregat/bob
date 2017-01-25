package be.grgt.jonas.bob.processor;


import be.grgt.jonas.bob.Buildable;
import be.grgt.jonas.bob.definitions.TypeDefinition;
import be.grgt.jonas.bob.factories.BuildableSourceDefinitionFactory;
import be.grgt.jonas.bob.generators.BuilderGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("be.grgt.jonas.bob.Buildable")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public final class BuildableProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Elements elementUtils = processingEnv.getElementUtils();

        BuilderGenerator builderGenerator = new BuilderGenerator(processingEnv.getFiler());

        BuildableSourceDefinitionFactory sourceDefinitionFactory = new BuildableSourceDefinitionFactory(elementUtils);

        for (Element elem : roundEnv.getElementsAnnotatedWith(Buildable.class)) {
            Buildable buildable = elem.getAnnotation(Buildable.class);
            TypeDefinition sourceDefinition = sourceDefinitionFactory.produce(elem);
            builderGenerator.generate(sourceDefinition, buildable);
        }
        return true;
    }

}