package dev.declaration.processor;

import dev.declaration.module.Exclude;
import dev.declaration.http.*;
import io.immutables.codec.record.meta.Inline;
import io.immutables.meta.Null;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.TypeElement;

class KnownAnnotations {
	private final Map<Class<? extends Annotation>, Object> annotations;

	KnownAnnotations(Map<Class<? extends Annotation>, Object> annotations) {
		this.annotations = annotations;
	}

	static final Set<Class<? extends Annotation>> httpMethods = Set.of(
		GET.class,
		PUT.class,
		POST.class,
		PATCH.class,
		DELETE.class,
		OPTIONS.class
	);

	private static final Set<Class<? extends Annotation>> other = Set.of(
		Path.class,
		Status.class,

		Exclude.class,
		Null.class,
		Inline.class
	);

	private final static Map<String, Class<? extends Annotation>> knownByName;
	static {
		var byName = new HashMap<String, Class<? extends Annotation>>();
		for (var c : httpMethods) byName.put(c.getCanonicalName(), c);
		for (var c : other) byName.put(c.getCanonicalName(), c);
		knownByName = Map.copyOf(byName);
	}

	boolean has(Class<? extends Annotation> type) {
		return annotations.containsKey(type);
	}

	Set<Class<? extends Annotation>> present() {
		return annotations.keySet();
	}

	<A extends Annotation> @Null A get(Class<? extends A> type) {
		return type.cast(annotations.get(type));
	}

	static KnownAnnotations from(AnnotatedConstruct annotated) {
		var found = new HashMap<Class<? extends Annotation>, Object>();
		for (var a : annotated.getAnnotationMirrors()) {
			var typeElement = (TypeElement) a.getAnnotationType().asElement();
			@Null var type =
				knownByName.get(typeElement.getQualifiedName().toString());
			if (type != null) {
				Annotation synthesized = annotated.getAnnotation(type);
				assert synthesized != null;
				found.put(type, synthesized);
			}
		}
		return new KnownAnnotations(found);
	}

	static final KnownAnnotations Empty = new KnownAnnotations(Map.of());
}
