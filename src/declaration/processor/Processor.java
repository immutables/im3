package io.immutables.declaration.processor;

import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import static javax.tools.Diagnostic.Kind.ERROR;

// Since we require records, it should be safe to require LTS release 17
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes(Processor.ANNOTATION_API_INVENTORY)
@SupportedOptions({
	Processor.OPTION_X
})
public class Processor extends AbstractProcessor {
	static final String ANNOTATION_API_INVENTORY = "io.immutables.declaration.ApiInventory";
	static final String OPTION_X = "io.immutables.declaration.x";

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
		if (round.processingOver()) {
			// processingEnv.getMessager().printMessage(NOTE,"HERE HERE HERE A B C !");
		} else {
			var elements = processingEnv.getElementUtils();
			var annotationElement = elements.getTypeElement(ANNOTATION_API_INVENTORY);
			var inventoryPackages = round.getElementsAnnotatedWith(annotationElement);

			for (var p : inventoryPackages) {
				var discoverer = new InventoryDiscoverer(processingEnv, p);
				discoverer.discover();
			}
		}
		// Regardless of what other processors might want, we claim our trigger
		// annotation which we put on a package level.
		return true;
	}

	private void writeFile() throws IOException {
		var kapow = processingEnv.getFiler().createSourceFile("ar.kan.sas.Kapow");
		try (var w = kapow.openWriter()) {
			w.write("""
				package ar.kan.sas;
								
				public enum Kapow { ThisIsIt }
				""");
		}
	}
}
