package io.immutables.declaration.processor;

import io.immutables.meta.Null;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
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

			var kind = element.getKind();
			switch (kind) {
			case ENUM -> datatypeEnum((TypeElement) element);
			case RECORD -> datatypeRecord((TypeElement) element);
			case INTERFACE -> {
				var type = (TypeElement) element;
				if (isContractKind(type)) {
					contractInterface(type);
				} else if (type.getModifiers().contains(Modifier.SEALED)) {
					datatypeSealedInterface(type);
				} else {
					// can also be mixin interface, we don't consider it as a problem,
					// and we don't process it either
				}
			}
			default -> processing.getMessager().printMessage(Diagnostic.Kind.ERROR,
				"Not supported element of kind: " + kind, element);
			}
		}
	}

	private static boolean isContractKind(TypeElement type) {
		return findAnnotation(type, ANNOTATION_HTTP_PATH) != null;
	}

	private void contractInterface(TypeElement type) {
		processing.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
			"Found contract " + type.getSimpleName(), type);
	}

	private void datatypeRecord(TypeElement type) {
		//ElementFilter.recordComponentsIn(element.getEnclosedElements())

		processing.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
			"Found record " + type.getSimpleName(), type);
	}

	private void datatypeSealedInterface(TypeElement type) {

		processing.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
			"Found sealed " + type.getSimpleName(), type);
	}

	private void datatypeEnum(TypeElement type) {
		processing.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
			"Found enum " + type.getSimpleName(), type);
	}

	private boolean excluded(Element element) {
		return findAnnotation(element, ANNOTATION_EXCLUDE) != null;
	}

	private static @Null AnnotationMirror findAnnotation(Element element, String annotationType) {
		for (var annotation : element.getAnnotationMirrors()) {
			var typeElement = (TypeElement) annotation.getAnnotationType().asElement();
			if (typeElement.getQualifiedName().contentEquals(annotationType)) {
				return annotation;
			}
		}
		return null;
	}

	public static final String ANNOTATION_EXCLUDE = "io.immutables.declaration.Exclude";
	public static final String ANNOTATION_HTTP_PATH = "io.immutables.declaration.http.Path";
	//public static final String ANNOTATION_EXCLUDE = "io.immutables.declaration.Exclude";
}
