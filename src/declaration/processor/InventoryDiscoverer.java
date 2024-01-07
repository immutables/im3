package dev.declaration.processor;

import dev.declaration.Exclude;
import dev.declaration.http.Path;
import dev.declaration.processor.ContractIntrospector;

import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

class InventoryDiscoverer {
	private final ProcessingEnvironment processing;
	private final ContractIntrospector contracts;

	private final DatatypeIntrospector datatypes;

	private final Map<AnnotatedConstruct, KnownAnnotations> annotationsCache = new HashMap<>();

	InventoryDiscoverer(ProcessingEnvironment processing) {
		this.processing = processing;
		this.datatypes = new DatatypeIntrospector(processing, annotationsCache);
		this.contracts = new ContractIntrospector(processing, datatypes, annotationsCache);
	}

	Declaration.Module discover(PackageElement inventoryPackage) {
		var name = inventoryPackage.getQualifiedName().toString();
		// we want to have well-defined order, but that may not necessarily
		// be achievable just by how compiler provides elements to us, so we're
		// imposing alphabetical order on a name, expecting, of course,
		// the same single module.
		var declarations = new TreeSet<>(
			Comparator.comparing(Declaration::name));

		for (var element : inventoryPackage.getEnclosedElements()) {
			var annotations = annotationsCache.computeIfAbsent(element, KnownAnnotations::from);

			if (annotations.has(Exclude.class)) continue;

			switch (element.getKind()) {
			case ENUM, RECORD -> {
				var type = (TypeElement) element;
				datatypes.introspect(type).ifPresent(declarations::add);
				discoverInnerDatatypes(type.getEnclosedElements(), declarations);
			}
			case INTERFACE -> {
				var type = (TypeElement) element;
				if (annotations.has(Path.class)) {
					contracts.introspect(type).ifPresent(declarations::add);
					discoverInnerDatatypes(type.getEnclosedElements(), declarations);
				} else if (type.getModifiers().contains(Modifier.SEALED)) {
					datatypes.introspect(type).ifPresent(declarations::add);
					discoverInnerDatatypes(type.getEnclosedElements(), declarations);
				} else {
					// can also be mixin interface, we don't consider it as a problem,
					// and we don't process it either
				}
			}
			default -> processing.getMessager().printMessage(Diagnostic.Kind.ERROR,
				"Not supported element of container: " + element.getKind(), element);
			}
		}

		return new Declaration.Module(
			name,
			List.copyOf(declarations),
			datatypes.commentOf(inventoryPackage).lines()
		);
	}

	// while introspection discovers all referenced types, we do
	// separate recursion into inner types here and possibly will
	// find many types already introspected.
	// For such manual recursion we have separate self-recursive method
	// with some minimal duplication. We will look only for nested datatypes
	private void discoverInnerDatatypes(
		List<? extends Element> enclosedElements, SortedSet<Declaration> declarations) {

		for (var element : enclosedElements) {
			var annotations = annotationsCache.computeIfAbsent(element, KnownAnnotations::from);
			if (annotations.has(Exclude.class)) continue;
			// we allow here other kinds - methods etc, not erroring on those, only looking
			// for data types
			switch (element.getKind()) {
			case ENUM, RECORD -> {
				var type = (TypeElement) element;
				datatypes.introspect(type).ifPresent(declarations::add);
				discoverInnerDatatypes(type.getEnclosedElements(), declarations);
			}
			case INTERFACE -> {
				var type = (TypeElement) element;
				if (type.getModifiers().contains(Modifier.SEALED)) {
					datatypes.introspect(type).ifPresent(declarations::add);
					discoverInnerDatatypes(type.getEnclosedElements(), declarations);
				}
			}
			}
		}
	}
}
