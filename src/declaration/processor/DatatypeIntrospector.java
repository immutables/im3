package io.immutables.declaration.processor;

import io.immutables.common.Vect;
import io.immutables.meta.Inline;
import io.immutables.meta.InsertOrder;
import io.immutables.meta.Null;
import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

class DatatypeIntrospector {
	private final ProcessingEnvironment processing;
	private final Elements elements;
	private final Types types;

	private int variableCounter;

	final Map<String, Declaration> declarations = new HashMap<>();
	final Map<AnnotatedConstruct, KnownAnnotations> annotationsCache;

	private final Map<String, Type> predeclaredTypes = new HashMap<>();
	{
		predeclaredTypes.put(String.class.getName(), Type.Primitive.String);

		predeclaredTypes.put(OptionalInt.class.getName(),
			new Type.Container(Type.Container.Kind.OptionalPrimitive, Type.Primitive.Integer));
		predeclaredTypes.put(OptionalLong.class.getName(),
			new Type.Container(Type.Container.Kind.OptionalPrimitive, Type.Primitive.Long));
		predeclaredTypes.put(OptionalDouble.class.getName(),
			new Type.Container(Type.Container.Kind.OptionalPrimitive, Type.Primitive.Float));
	}

	private final Map<String, Type.Container.Kind> containerTypes = new HashMap<>();
	{
		containerTypes.put(Optional.class.getName(), Type.Container.Kind.Optional);
		containerTypes.put(List.class.getName(), Type.Container.Kind.List);
		containerTypes.put(Set.class.getName(), Type.Container.Kind.Set);
	}

	record ReferenceInContext(Declaration.Reference reference, Element inContext) {}

	private final List<ReferenceInContext> toValidate = new ArrayList<>();

	DatatypeIntrospector(
		ProcessingEnvironment processing,
		Map<AnnotatedConstruct, KnownAnnotations> annotationsCache) {

		this.processing = processing;
		this.elements = processing.getElementUtils();
		this.types = processing.getTypeUtils();
		this.annotationsCache = annotationsCache;

		collectBoxedTypes();
	}

	private void collectBoxedTypes() {
		addBoxedType(TypeKind.BOOLEAN, Type.Primitive.Boolean);
		addBoxedType(TypeKind.INT, Type.Primitive.Integer);
		addBoxedType(TypeKind.LONG, Type.Primitive.Long);
		addBoxedType(TypeKind.DOUBLE, Type.Primitive.Float);
		addBoxedType(TypeKind.BOOLEAN, Type.Primitive.Boolean);
	}

	private void addBoxedType(TypeKind kind, Type.Primitive primitive) {
		assert kind.isPrimitive();
		var boxed = types.boxedClass(types.getPrimitiveType(kind));
		var qualifiedName = boxed.getQualifiedName().toString();
		predeclaredTypes.put(qualifiedName, primitive);
	}

	Optional<Declaration> introspect(TypeElement element) {
		String qualifiedName = element.getQualifiedName().toString();
		return Optional.ofNullable(
			declarations.computeIfAbsent(qualifiedName, k -> declarationFrom(element)));
	}

	private @Null Declaration declarationFrom(TypeElement element) {
		return switch (element.getKind()) {
			case ENUM -> enumFrom(element);
			case RECORD -> recordFrom(element);
			case INTERFACE -> sealedFrom(element);
			default -> null;
		};
	}

	private @Null Declaration sealedFrom(TypeElement element) {
		if (!element.getModifiers().contains(Modifier.SEALED)) return null;

		var typeVariables = mapTypeVariables(element);
		var reference = reference(element);

		var cases = element.getPermittedSubclasses().stream()
			.map(t -> (TypeElement) types.asElement(t))
			.map(this::introspect)
			.<Declaration>mapMulti(Optional::ifPresent)
			.toList();

		matchTypeVariables(element, reference, cases, typeVariables);

		return new Declaration.Sealed(
			Declaration.Sealed.Tag.Is,
			reference,
			List.copyOf(typeVariables.values()),
			cases,
			commentOf(element));
	}

	private void matchTypeVariables(
		TypeElement element,
		Declaration.Reference sealed,
		List<Declaration> cases,
		Map<String, Type.Variable> variables) {

		// TODO more precise validation, now it's just by name!!
		var sealedParams = variables.values()
			.stream()
			.map(Type.Variable::name)
			.toList();

		for (var c : cases) {
			// only Parameterizable declarations are expected in cases
			var caseParams = ((Declaration.Parameterizable) c).parameters()
				.stream()
				.map(Type.Variable::name)
				.toList();

			if (!caseParams.equals(sealedParams)) {
				error("Case type parameters of %s %s mismatch with sealed interface %s %s"
					.formatted(show(c), caseParams, show(sealed), sealedParams), element);
			}
		}
	}

	private static String show(Declaration declaration) {
		return show(declaration.reference());
	}

	private static String show(Declaration.Reference r) {
		return r.module() + ":" + r.name();
	}

	private Declaration recordFrom(TypeElement element) {
		var annotations = knownAnnotationsOf(element);
		var isInline = annotations.has(Inline.class);

		var reference = reference(element);
		var typeVariables = mapTypeVariables(element);
		var vars = List.copyOf(typeVariables.values());

		var components = new ArrayList<Declaration.Component>();
		for (RecordComponentElement c : element.getRecordComponents()) {
			components.add(componentFrom(c, typeVariables));
		}

		Declaration declaration;

		if (isInline) {
			if (components.size() == 1) {
				declaration = new Declaration.Inline(
					Declaration.Inline.Tag.Is,
					reference, vars, components.get(0), commentOf(element));
			} else {
				declaration = new Declaration.Product(
					Declaration.Product.Tag.Is,
					reference, vars, List.copyOf(components), commentOf(element));
			}
		} else {
			declaration = new Declaration.Record(
				Declaration.Record.Tag.Is,
				reference, vars, List.copyOf(components), commentOf(element));
		}

		return declaration;
	}

	/**
	 * In general, a string key is not precise in a complex scenarios,
	 * but in case of data records, it should give good result, i.e. good match
	 */
	private @InsertOrder Map<String, Type.Variable> mapTypeVariables(
		TypeElement element) {

		if (element.getTypeParameters().isEmpty()) return Map.of();

		var variables = new LinkedHashMap<String, Type.Variable>();
		for (var p : element.getTypeParameters()) {
			var name = p.getSimpleName().toString();
			variables.put(name, allocateVariable(name));
		}

		return variables;
	}

	private Declaration enumFrom(TypeElement element) {
		var constants = element.getEnclosedElements().stream()
			.filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
			.map(e -> e.getSimpleName().toString())
			.map(Declaration.Enum.Constant::new)
			.toList();

		return new Declaration.Enum(
			Declaration.Enum.Tag.Is,
			reference(element),
			constants,
			commentOf(element));
	}

	private Type.Variable allocateVariable(String name) {
		return new Type.Variable(++variableCounter, name);
	}

	private Declaration.Component componentFrom(
		RecordComponentElement component,
		Map<String, Type.Variable> typeVariables) {

		//component.getAccessor().getReturnType();
		var typeMirror = component.asType();

		var decoder = new TypeDecoder(
			component,
			typeMirror,
			typeVariables);

		var name = component.getSimpleName().toString();
		Type type = decoder.decode(knownAnnotationsOf(component));

		return new Declaration.Component(
			name, type, typeMirror.toString(), commentOf(component));
	}

	class TypeDecoder {
		final Element element;
		final TypeMirror start;
		final Map<String, Type.Variable> variables;

		TypeDecoder(Element element, TypeMirror start, Map<String, Type.Variable> variables) {
			this.element = element;
			this.start = start;
			this.variables = variables;
		}

		Type decode(KnownAnnotations annotations) {
			return decode(start, annotations);
		}

		Type decode(TypeMirror type, KnownAnnotations elementAnnotations) {
			// type annotations are largerly ignored
			var typeUseAnnotations = knownAnnotationsOf(type);
			boolean nullComponent = elementAnnotations.has(Null.class);

			var kind = type.getKind();

			if (kind.isPrimitive() && nullComponent) {
				error("Do not use @%s annotation on a primitive type '%s'"
					.formatted(Null.class.getSimpleName(), type), element);
			}

			return switch (kind) {
				// Null and void will not normally be encountered in records
				// but for handling contract return types and some other places (speculating)
				case NULL -> Type.Primitive.Null;
				case VOID -> Type.Primitive.Void;

				case BOOLEAN -> Type.Primitive.Boolean;
				case DOUBLE -> Type.Primitive.Float;
				case INT -> Type.Primitive.Integer;
				case LONG -> Type.Primitive.Long;

				case BYTE, SHORT -> unsupported(type, element, "Use 'int' instead");
				case FLOAT -> unsupported(type, element, "Use 'double' instead");
				case CHAR -> unsupported(type, element, "Use 'String' instead");

				case DECLARED -> {
					var declared = (DeclaredType) type;
					yield decodeDeclared(declared, (TypeElement) declared.asElement(),
						elementAnnotations, typeUseAnnotations);
				}

				case TYPEVAR -> {
					var name = ((TypeVariable) type).asElement().getSimpleName().toString();
					@Null var v = variables.get(name);
					yield v != null ? v : unsupported(type, element, "Unmapped type variable");
				}

				case ARRAY -> unsupported(type, element, "Use 'List' or 'Set' instead");
				case WILDCARD -> unsupported(type, element, "Replace wildcard with just an element");

				case OTHER, NONE, ERROR, PACKAGE, EXECUTABLE, UNION, INTERSECTION, MODULE ->
					unsupported(type, element, "Unexpected here");
			};
		}

		private Type decodeDeclared(
			DeclaredType type, TypeElement typeElement,
			KnownAnnotations elementAnnotations, KnownAnnotations typeUseAnnotations) {

			var qualifiedName = typeElement.getQualifiedName().toString();

			@Null var predeclared = predeclaredTypes.get(qualifiedName);
			if (predeclared != null) {
				return wrapByAnnotations(
					predeclared,
					elementAnnotations, typeUseAnnotations);
			}

			@Null var containerType = containerTypes.get(qualifiedName);
			if (containerType != null) {
				@Null var a = requiredTypeArgument(type, 0);
				// just error propagation, need to replace with special Type.Error ?
				if (a == null) return Type.Primitive.Void;
				// when we do a recursive type decode, we let go of element annotations
				Type argumentType = decode(a, KnownAnnotations.Empty);

				return wrapByAnnotations(
					new Type.Container(containerType, argumentType),
					elementAnnotations, typeUseAnnotations);
			}

			if (!type.getTypeArguments().isEmpty()) {
				var reference = reference(typeElement);
				// recursive call here, so lambda/stream pipeline will make it worse
				var arguments = new ArrayList<Type>();
				for (var a : type.getTypeArguments()) {
					arguments.add(decode(a, KnownAnnotations.Empty));
				}
				return wrapByAnnotations(
					new Type.Applied(reference, List.copyOf(arguments)),
					elementAnnotations, typeUseAnnotations);
			}

			var reference = reference(typeElement);

			return wrapByAnnotations(
				new Type.Terminal(reference),
				elementAnnotations, typeUseAnnotations);
		}

		private Declaration.Reference reference(TypeElement typeElement) {
			String module = moduleOf(typeElement);
			String name = nameOf(typeElement);
			var reference = new Declaration.Reference(module, name);
			enque(reference);
			return reference;
		}

		private void enque(Declaration.Reference reference) {
			// Idea here is that we don't check if referenced type is a correct
			// datatype (recursively), we just remember it here,
			// and then, at some point, presumably, when we have
			// collected as much information as needed
			toValidate.add(new ReferenceInContext(reference, element));
		}

		private Type wrapByAnnotations(Type argument,
			KnownAnnotations elementAnnotations, KnownAnnotations typeUseAnnotations) {
			// TODO handle or prohibit @Null List? @Null Set @Null Optional

			if (elementAnnotations.has(Null.class)) {
				return new Type.Container(Type.Container.Kind.Nullable, argument);
			}
			return argument;
		}

		private @Null TypeMirror requiredTypeArgument(DeclaredType type, int index) {
			var arguments = type.getTypeArguments();
			assert index >= 0;
			if (index < arguments.size()) return arguments.get(index);

			error("Missing required type argument in %s [%d]".formatted(type, index), element);
			return null;
		}

		private Type unsupported(TypeMirror type, Element element, String explain) {
			error("Unsupported type %s (%s). %s".formatted(type, type.getKind(), explain), element);
			return Type.Primitive.Void; // just to return something
		}
	}

	Declaration.Reference reference(TypeElement element) {
		return new Declaration.Reference(moduleOf(element), nameOf(element));
	}

	private String moduleOf(TypeElement element) {
		return elements.getPackageOf(element).getQualifiedName().toString();
	}

	private String nameOf(TypeElement element) {
		var name = Vect.<String>of();
		for (Element e = element; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
			name = name.prepend(e.getSimpleName().toString());
		}
		return name.join(".");
	}

	private void error(String message, Element element) {
		processing.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
	}

	private KnownAnnotations knownAnnotationsOf(AnnotatedConstruct type) {
		return annotationsCache.computeIfAbsent(type, KnownAnnotations::from);
	}

	String commentOf(Element element) {
		@Null String c = elements.getDocComment(element);
		return c != null ? c : "";
	}
}
