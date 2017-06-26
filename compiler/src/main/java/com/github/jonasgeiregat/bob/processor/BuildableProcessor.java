package com.github.jonasgeiregat.bob.processor;


import com.github.jonasgeiregat.bob.Buildable;
import com.github.jonasgeiregat.bob.definitions.TypeDefinition;
import com.github.jonasgeiregat.bob.factories.BuildableSourceDefinitionFactory;
import com.github.jonasgeiregat.bob.generators.BuilderGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Set;

@SupportedAnnotationTypes("com.github.jonasgeiregat.bob.Buildable")
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