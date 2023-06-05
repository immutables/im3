package io.immutables.declaration.processor;

import io.immutables.meta.Null;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.tools.Diagnostic;

class Annotations {
	private Annotations() {}

	static @Null AnnotationMirror findAnnotation(AnnotatedConstruct annotated,
		String annotationType) {
		for (var a : annotated.getAnnotationMirrors()) {
			var typeElement = (TypeElement) a.getAnnotationType().asElement();
			if (typeElement.getQualifiedName().contentEquals(annotationType)) {
				return a;
			}
		}
		return null;
	}

	static @Null String getAttribute(AnnotationMirror annotation, String attribute) {
		for (var e : annotation.getElementValues().entrySet()) {
			if (e.getKey().getSimpleName().contentEquals(attribute)) {
				return e.getValue().accept(attributeValueStringifier, null);
			}
		}
		return null;
	}

	static final String ANNOTATION_HTTP_PATH = "io.immutables.declaration.http.Path";
	static final String ANNOTATION_HTTP_STATUS = "io.immutables.declaration.http.Status";

	static final String ANNOTATION_HTTP_GET = "io.immutables.declaration.http.GET";
	static final String ANNOTATION_HTTP_PUT = "io.immutables.declaration.http.PUT";
	static final String ANNOTATION_HTTP_POST = "io.immutables.declaration.http.POST";
	static final String ANNOTATION_HTTP_PATCH = "io.immutables.declaration.http.PATCH";
	static final String ANNOTATION_HTTP_DELETE = "io.immutables.declaration.http.DELETE";
	static final String ANNOTATION_HTTP_OPTIONS = "io.immutables.declaration.http.OPTIONS";

	static final String ANNOTATION_EXCLUDE = "io.immutables.declaration.Exclude";
	static final String ANNOTATION_META_INLINE = "io.immutables.meta.Inline";
	static final String ANNOTATION_META_NULL = "io.immutables.meta.Null";

	/** Ensures that our constants in names refers to actual annotation classes */
	@Deprecated
	static void ensureAnnotationsExist(ProcessingEnvironment processing) {
		for (var f : Annotations.class.getDeclaredFields()) {
			if (isAnnotationConstant(f)) {
				String className;
				try {
					className = (String) f.get(null);
				} catch (IllegalAccessException e) {
					processing.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
					continue;
				}

				@Null TypeElement element = processing.getElementUtils().getTypeElement(className);

				if (element == null) {
					processing.getMessager().printMessage(Diagnostic.Kind.ERROR,
						"Cannot find %s on the compilation classpath".formatted(className));
					continue;
				}

				if (element.getKind() != ElementKind.ANNOTATION_TYPE) {
					processing.getMessager().printMessage(Diagnostic.Kind.ERROR,
						"Not an annotation type: %s".formatted(className));
				}
			}
		}
	}

	private static boolean isAnnotationConstant(Field f) {
		return Modifier.isStatic(f.getModifiers())
			&& f.getType() == String.class
			&& f.getName().startsWith("ANNOTATION_");
	}

	private static final SimpleAnnotationValueVisitor14<String, Void> attributeValueStringifier =
		new SimpleAnnotationValueVisitor14<>("-N/A-") {
			@Override public String visitType(TypeMirror t, Void unused) {
				return t.toString();
			}

			@Override public String visitAnnotation(AnnotationMirror a, Void unused) {
				return a.toString();
			}

			@Override public String visitEnumConstant(VariableElement c, Void unused) {
				return c.getSimpleName().toString();
			}

			@Override public String visitInt(int i, Void unused) {
				return String.valueOf(i);
			}

			@Override public String visitLong(long l, Void unused) {
				return String.valueOf(l);
			}

			@Override public String visitBoolean(boolean b, Void unused) {
				return String.valueOf(b);
			}

			@Override public String visitString(String s, Void unused) {
				return s;
			}

			// TODO other
		};
}
