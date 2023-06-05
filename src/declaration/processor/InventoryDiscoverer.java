package io.immutables.declaration.processor;

import io.immutables.declaration.Exclude;
import io.immutables.declaration.http.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

class InventoryDiscoverer {
	private final ProcessingEnvironment processing;
	private final ContractIntrospector contracts;

	private final DatatypeIntrospector datatypes;

	final Map<AnnotatedConstruct, KnownAnnotations> annotationsCache = new HashMap<>();

	InventoryDiscoverer(ProcessingEnvironment processing) {
		this.processing = processing;
		this.datatypes = new DatatypeIntrospector(processing, annotationsCache);
		this.contracts = new ContractIntrospector(processing, datatypes, annotationsCache);
	}

	List<Declaration> discover(Element inventoryPackage) {
		var declarations = new ArrayList<Declaration>();

		for (var element : inventoryPackage.getEnclosedElements()) {
			var annotations = annotationsCache.computeIfAbsent(element, KnownAnnotations::from);

			if (annotations.has(Exclude.class)) continue;

			switch (element.getKind()) {
				case ENUM, RECORD -> {
					var type = (TypeElement) element;
					datatypes.introspect(type).ifPresent(declarations::add);
				}
				case INTERFACE -> {
					var type = (TypeElement) element;
					if (annotations.has(Path.class)) {
						contracts.introspect(type).ifPresent(declarations::add);
					} else if (type.getModifiers().contains(Modifier.SEALED)) {
						datatypes.introspect(type).ifPresent(declarations::add);
					} else {
						// can also be mixin interface, we don't consider it as a problem,
						// and we don't process it either
					}
				}
				default -> processing.getMessager().printMessage(Diagnostic.Kind.ERROR,
					"Not supported element of container: " + element.getKind(), element);
			}
		}

		return declarations;
	}
}
