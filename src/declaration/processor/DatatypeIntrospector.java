package dev.declaration.processor;

import io.immutables.meta.Null;
import io.immutables.codec.record.meta.Inline;
import java.net.URI;
import java.time.*;
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

		predeclaredTypes.put(UUID.class.getName(), Type.Extended.Uuid);
		predeclaredTypes.put(URI.class.getName(), Type.Extended.Uri);
		predeclaredTypes.put(Instant.class.getName(), Type.Extended.Instant);
		predeclaredTypes.put(LocalDate.class.getName(), Type.Extended.LocalDate);
		predeclaredTypes.put(LocalTime.class.getName(), Type.Extended.LocalTime);
		predeclaredTypes.put(LocalDateTime.class.getName(), Type.Extended.LocalDateTime);
		predeclaredTypes.put(OffsetDateTime.class.getName(), Type.Extended.OffsetDateTime);
		// Temporarily!!
		predeclaredTypes.put("platform.JsonAny", Type.Extended.Any);
		predeclaredTypes.put("platform.JsonAny.Struct", Type.Extended.MapAny);
		// Extended.Bytes are handled in a different way, see switches on types

		// VOID? ANY?
		predeclaredTypes.put(Object.class.getName(), Type.Primitive.Void);
	}

	private final Map<String, Type.Container.Kind> containerTypes = new HashMap<>();
	{
		containerTypes.put(Optional.class.getName(), Type.Container.Kind.Optional);
		containerTypes.put(List.class.getName(), Type.Container.Kind.List);
		containerTypes.put(Set.class.getName(), Type.Container.Kind.Set);
	}

	record ReferenceInContext(Reference reference, Element inContext) {}

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
		// we don't use computeIfAbsent here because of nested logic which does
		// introspection too, and we can get ConcurrentModificationException
		@Null var declaration = declarations.get(qualifiedName);
		// can be null for unsupported type kind, so we check for entry
		if (!declarations.containsKey(qualifiedName)) {
			declaration = declarationFrom(element);
			declarations.put(qualifiedName, declaration);
		}
		return Optional.ofNullable(declaration);
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
			.mapMulti(Optional::ifPresent)
			.map(Declaration.Datatype.class::cast)
			.toList();

		matchTypeVariables(element, reference, cases, typeVariables);

		return new Declaration.Sealed(
			Declaration.Sealed.Tag.Is,
			reference,
			List.copyOf(typeVariables.values()),
			cases,
			commentOf(element).lines());
	}

	private void matchTypeVariables(
		TypeElement element,
		Reference sealed,
		List<Declaration.Datatype> cases,
		Map<String, Type.Variable> variables) {

		if (variables.isEmpty()) return; // no variables - nothing to match

		// TODO more precise validation, now it's just by name!!
		var sealedParams = variables.values()
			.stream()
			.map(Type.Variable::name)
			.toList();

		for (var c : cases) {
			List<String> caseParams;
			if (c instanceof Declaration.Parameterizable parameterizable) {
				caseParams = parameterizable.parameters()
					.stream()
					.map(Type.Variable::name)
					.toList();
			} else {
				caseParams = List.of();
			}

			if (!caseParams.equals(sealedParams)) {
				error("Case type parameters of %s %s mismatch with sealed interface %s %s"
					.formatted(show(c), caseParams, show(sealed), sealedParams), element);
			}
		}
	}

	private static String show(Declaration declaration) {
		return show(declaration.reference());
	}

	private static String show(Reference r) {
		return r.module() + ":" + r.name();
	}

	private Declaration recordFrom(TypeElement element) {
		var annotations = knownAnnotationsOf(element);
		var isInline = annotations.has(Inline.class);

		var reference = reference(element);
		var typeVariables = mapTypeVariables(element);
		var vars = List.copyOf(typeVariables.values());

		var recordComment = commentOf(element);

		var components = new ArrayList<Declaration.Component>();
		for (RecordComponentElement c : element.getRecordComponents()) {
			components.add(componentFrom(c, typeVariables, recordComment));
		}

		Declaration declaration;

		if (isInline) {
			if (components.size() == 1) {
				var inlinedComponentComment = commentOf(element.getRecordComponents().get(0));
				var comment = DocComment.concat(
					recordComment.lines(),
					inlinedComponentComment.lines());

				declaration = new Declaration.Inline(
					Declaration.Inline.Tag.Is,
					reference, vars, components.get(0), comment);

			} else {
				// TODO here we don't consider any lines on components
				declaration = new Declaration.Product(
					Declaration.Product.Tag.Is,
					reference, vars, List.copyOf(components), recordComment.lines());
			}
		} else {
			declaration = new Declaration.Record(
				Declaration.Record.Tag.Is,
				reference, vars, List.copyOf(components), recordComment.lines());
		}

		return declaration;
	}

	/**
	 * In general, a string key is not precise in a complex scenarios,
	 * but in case of data records, it should give good result, i.e. good match.
	 * Returned map maintains insertion ordering
	 */
	private Map<String, Type.Variable> mapTypeVariables(
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
		var constants = new ArrayList<Declaration.Enum.Constant>();

		for (var e : element.getEnclosedElements()) {
			if (e.getKind() == ElementKind.ENUM_CONSTANT) {
				var name = e.getSimpleName().toString();
				var comment = commentOf(e).lines();

				constants.add(new Declaration.Enum.Constant(
					name, comment));
			}
		}

		return new Declaration.Enum(
			Declaration.Enum.Tag.Is,
			reference(element),
			constants,
			commentOf(element).lines());
	}

	private Type.Variable allocateVariable(String name) {
		return new Type.Variable(++variableCounter, name);
	}

	private Declaration.Component componentFrom(
		RecordComponentElement component,
		Map<String, Type.Variable> typeVariables,
		DocComment recordComment) {

		//component.getAccessor().getReturnType();
		var typeMirror = component.asType();

		var decoder = new TypeRecognizer(
			component,
			typeMirror,
			typeVariables);

		var name = component.getSimpleName().toString();
		Type type = decoder.decode(knownAnnotationsOf(component));

		var comment = extractComponentComment(component, recordComment, name);

		return new Declaration.Component(
			name, type, typeMirror.toString(), comment);
	}

	private List<String> extractComponentComment(
		RecordComponentElement component, DocComment recordComment, String name) {
		// Here we can consider that component might have its own comment,
		// but it might be just part of @param taglet on the record comment
		// Currently I don't understand how it's specified, so we try both
		DocComment componentComment = commentOf(component);
		List<String> comment = componentComment.lines();
		if (comment.isEmpty()) {
			comment = recordComment.parameters().getOrDefault(name, List.of());
		}
		return comment;
	}

	Type interpretType(TypeElement enclosing, Element element) {
 		var typeVariables = mapTypeVariables(enclosing);
		var recognizer = new TypeRecognizer(
			element,
			element.asType(),
			typeVariables);
		return recognizer.decode(knownAnnotationsOf(element));
	}

	Type interpretType(TypeElement enclosing, TypeMirror type, Element element) {
		var typeVariables = mapTypeVariables(enclosing);
		var recognizer = new TypeRecognizer(
			element,
			type,
			typeVariables);
		return recognizer.decode(knownAnnotationsOf(element));
	}

	Type interpretType(TypeElement enclosing, TypeMirror type, Element element, KnownAnnotations annotations) {
		var typeVariables = mapTypeVariables(enclosing);
		var recognizer = new TypeRecognizer(
			element,
			type,
			typeVariables);
		return recognizer.decode(annotations);
	}

	class TypeRecognizer {
		final Element element;
		final TypeMirror start;
		final Map<String, Type.Variable> variables;

		TypeRecognizer(Element element, TypeMirror start, Map<String, Type.Variable> variables) {
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

				case ARRAY -> {
					if (((ArrayType) type).getComponentType().getKind() == TypeKind.BYTE) {
						yield Type.Extended.Bytes;
					}
					yield unsupported(type, element, "Use 'List' or 'Set' instead");
				}

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

			@Null var containerKind = containerTypes.get(qualifiedName);
			if (containerKind != null) {
				@Null var a = requiredTypeArgument(type, 0);
				// just error propagation, need to replace with special Type.Error ?
				if (a == null) return Type.Primitive.Void;
				// when we do a recursive type decode, we let go of element annotations
				var elementType = decode(a, KnownAnnotations.Empty);
				var containerType = new Type.Container(containerKind, elementType);
				return wrapByAnnotations(containerType, elementAnnotations, typeUseAnnotations);
			}

			if (!type.getTypeArguments().isEmpty()) {
				var reference = reference(typeElement);
				// recursive call here, so lambda/stream pipeline will make it worse
				var arguments = new ArrayList<Type>();
				for (var a : type.getTypeArguments()) {
					arguments.add(decode(a, KnownAnnotations.Empty));
				}

				var appliedType = new Type.Applied(reference, List.copyOf(arguments));
				return wrapByAnnotations(appliedType, elementAnnotations, typeUseAnnotations);
			}

			var terminalType = new Type.Terminal(reference(typeElement));
			return wrapByAnnotations(terminalType, elementAnnotations, typeUseAnnotations);
		}

		private Reference reference(TypeElement typeElement) {
			String module = moduleOf(typeElement);
			String name = nameOf(typeElement);
			var reference = new Reference(module, name);
			enque(reference);
			return reference;
		}

		private void enque(Reference reference) {
			// Idea here is that we don't check if referenced type is a correct
			// datatype (recursively), we just remember it here,
			// and then, at some point, presumably, when we have
			// collected as much information as needed
			// FIXME toValidate is not read yet, this might not be an
			//  issue because dependency is validated by java compilation
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

	Reference reference(TypeElement element) {
		return new Reference(moduleOf(element), nameOf(element));
	}

	private String moduleOf(TypeElement element) {
		return elements.getPackageOf(element).getQualifiedName().toString();
	}

	private String nameOf(TypeElement element) {
		var nameParts = new ArrayList<CharSequence>();
		for (Element e = element; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
			nameParts.add(0, e.getSimpleName());
		}
		return String.join(".", nameParts);
	}

	private void error(String message, Element element) {
		processing.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
	}

	private KnownAnnotations knownAnnotationsOf(AnnotatedConstruct type) {
		return annotationsCache.computeIfAbsent(type, KnownAnnotations::from);
	}

	DocComment commentOf(Element element) {
		@Null String c = elements.getDocComment(element);
		return c != null ? DocComment.extract(c) : DocComment.Empty;
	}
}
