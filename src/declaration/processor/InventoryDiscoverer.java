package io.immutables.declaration.processor;

import io.immutables.meta.Null;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

class InventoryDiscoverer {
	private final ProcessingEnvironment processing;
	private final Element inventoryPackage;

	InventoryDiscoverer(ProcessingEnvironment processing, Element inventoryPackage) {
		this.processing = processing;
		this.inventoryPackage = inventoryPackage;
	}

	void discover() {
		for (var element : inventoryPackage.getEnclosedElements()) {
			if (excluded(element)) continue;

			switch (element.getKind()) {
			case ENUM:
				datatypeEnum(element);
				break;
			case RECORD:
				datatypeRecord(element);
				break;
			case INTERFACE:
				// can also be mixin interface
				if (findAnnotation(element, ANNOTATION_HTTP_PATH) != null) {
					contractInterface(element);
				}
				break;
			default:
				processing.getMessager().printMessage(Diagnostic.Kind.ERROR,
					"Not supported element of kind: ", element);
			}
		}
	}

	private void contractInterface(Element element) {
		processing.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
			"Found contract " + element.getSimpleName(), element);
	}

	private void datatypeRecord(Element element) {
		//ElementFilter.recordComponentsIn(element.getEnclosedElements())
		processing.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
			"Found record " + element.getSimpleName(), element);
	}

	private void datatypeEnum(Element element) {
		processing.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
			"Found enum " + element.getSimpleName(), element);
	}

	private boolean excluded(Element element) {
		return findAnnotation(element, ANNOTATION_EXCLUDE) != null;
	}

	private static @Null AnnotationMirror findAnnotation(Element element, String annotationType) {
		for (var a : element.getAnnotationMirrors()) {
			var type = (TypeElement) a.getAnnotationType().asElement();
			if (type.getQualifiedName().contentEquals(annotationType)) {
				return a;
			}
		}
		return null;
	}

	public static final String ANNOTATION_EXCLUDE = "io.immutables.declaration.Exclude";
	public static final String ANNOTATION_HTTP_PATH = "io.immutables.declaration.http.Path";
	//public static final String ANNOTATION_EXCLUDE = "io.immutables.declaration.Exclude";
}
