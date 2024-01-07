package dev.declaration.processor;

import io.immutables.meta.Null;
import dev.declaration.http.*;
import java.util.*;
import java.util.function.Consumer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

class ContractIntrospector {
	private final ProcessingEnvironment processing;
	private final Elements elements;
	private final Types types;
	private final DatatypeIntrospector datatypes;
	private final Map<AnnotatedConstruct, KnownAnnotations> annotationsCache;

	ContractIntrospector(
		ProcessingEnvironment processing,
		DatatypeIntrospector datatypes,
		Map<AnnotatedConstruct, KnownAnnotations> annotationsCache) {

		this.processing = processing;
		this.elements = processing.getElementUtils();
		this.types = processing.getTypeUtils();
		this.datatypes = datatypes;
		this.annotationsCache = annotationsCache;
	}

	Optional<Declaration.Contract> introspect(TypeElement type) {
		var name = type.getSimpleName().toString();

		assert type.getKind() == ElementKind.INTERFACE;

		if (!type.getTypeParameters().isEmpty()) {
			error("Contract '%s' cannot have type parameters".formatted(name), type);
			return Optional.empty();
		}

		if (type.getNestingKind() != NestingKind.TOP_LEVEL) {
			error("Contract '%s' cannot be a member type".formatted(name), type);
			return Optional.empty();
		}

		var contractAnnotations = knownAnnotationsOf(type);

		@Null Path path = contractAnnotations.get(Path.class);
		// FIXME path actually required, but not validated here?
		var pathPrefix = path != null ? path.value() : "";

		var contract = new Declaration.Contract(
			Declaration.Contract.Tag.Is,
			datatypes.reference(type),
			pathPrefix,
			extractOperations(type, pathPrefix),
			datatypes.commentOf(type).lines()
		);

		var qualifiedName = type.getQualifiedName().toString();
		// This is a bit illogical to go all the way introspecting
		// and only here realizing we could have it cached already,
		// but let it be for now, also we reach DatatypesIntrospector declaration cache.
		// This asymmetry definitely require refactoring.
		return Optional.of((Declaration.Contract)
			datatypes.declarations.computeIfAbsent(qualifiedName, q -> contract));
	}

	private KnownAnnotations knownAnnotationsOf(AnnotatedConstruct type) {
		return annotationsCache.computeIfAbsent(type, KnownAnnotations::from);
	}

	private void error(String message, Element element) {
		processing.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
	}

	private Map<String, Declaration.Operation> extractOperations(
		TypeElement type, String pathPrefix) {

		List<ExecutableElement> allMethods = List.copyOf( // make it eagerly computed
			ElementFilter.methodsIn(elements.getAllMembers(type)));

		var declaringType = (DeclaredType) type.asType();
		var operations = new LinkedHashMap<String, Declaration.Operation>(allMethods.size());

		for (var method : allMethods) {
			var name = method.getSimpleName().toString();

			if (((TypeElement) method.getEnclosingElement()).getQualifiedName()
				.contentEquals(Object.class.getName())) continue;

			var mirror = (ExecutableType) types.asMemberOf(declaringType, method);

			Set<Modifier> modifiers = method.getModifiers();

			// Maybe warn? probably not at the moment
			if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.NATIVE)) continue;

			boolean assumedValid = true;
			if (method.isDefault()) {
				error("Default methods not supported: '%s'".formatted(name), method);
				assumedValid = false;
			}

			if (!method.getTypeParameters().isEmpty()) {
				error("Operation cannot have type parameters: '%s'".formatted(name), method);
				assumedValid = false;
			}

			if (operations.containsKey(name)) {
				error("Duplicate (overloaded) operations not allowed: '%s'".formatted(name), method);
				assumedValid = false;
			}

			if (!assumedValid) continue;

			var annotations = knownAnnotationsOf(method);
			var binding = extractHttpBinding(annotations, method, pathPrefix);

			var operationComment = datatypes.commentOf(method);

			var returns = extractReturn(type, method, mirror, operationComment);
			var thrown = extractThrown(type, method, mirror, operationComment);

			var collectedParameters = new ArrayList<Declaration.Parameter>();
			var collectedFixedQuery = new ArrayList<Declaration.FixedQuery>();

			collectParameters(
				method, mirror, binding, operationComment,
				collectedParameters::add, collectedFixedQuery::add);

			operations.put(name, new Declaration.Operation(
				name,
				binding.template,
				binding.method,
				returns,
				thrown,
				List.copyOf(collectedParameters),
				List.copyOf(collectedFixedQuery),
				operationComment.lines()
			));
		}

		return Collections.unmodifiableMap(operations);
	}

	private void collectParameters(
		ExecutableElement method, ExecutableType mirror, HttpBinding binding,
		DocComment operationComment,
		Consumer<Declaration.Parameter> collectParameters,
		Consumer<Declaration.FixedQuery> collectFixedQuery) {

		// here we don't check if, for example, request entity is compatible
		// with certain HTTP method.
		var mappedParameterNames = new HashSet<String>();

		var uriParameters = binding.template.parameters;
		var parameterTypes = mirror.getParameterTypes();

		int i = 0;
		boolean hasBodyParameter = false;
		for (var p : method.getParameters()) {
			int index = i++;

			// compile-time name available, blessing, no dependency here on compiling with '-parameters'
			// as we need for runtime reflection on parameter names
			String name = p.getSimpleName().toString();

			@Null var uriParameter = uriParameters.get(name);
			Declaration.Parameter.Mapping mapping;
			String httpName;
			if (uriParameter != null) {
				mappedParameterNames.add(name);
				// we don't check duplicates here,
				// cannot compile a method with duplicate parameter names
				mapping = switch (uriParameter.kind()) {
					case Path -> Declaration.Parameter.Mapping.Path;
					case Query -> Declaration.Parameter.Mapping.Query;
				};
				httpName = uriParameter.httpName();
			} else {
				// assume this is body parameter
				if (!hasBodyParameter) {
					mappedParameterNames.add(name);
					hasBodyParameter = true;
					mapping = Declaration.Parameter.Mapping.Body;
				} else {
					error("Unmapped parameter '%s', cannot have more than one request body"
						.formatted(name), p);
					mapping = Declaration.Parameter.Mapping.Unmapped;
				}
				httpName = "";
			}

			var javaType = parameterTypes.get(index);
			var enclosingType = (TypeElement) method.getEnclosingElement();
			var decodedType = datatypes.interpretType(enclosingType, p);
			boolean required = isRequiredParameter(decodedType);

			collectParameters.accept(new Declaration.Parameter(
				name, httpName, index, decodedType, javaType, mapping, required,
				parameterComments(operationComment, p, name)));
		}

		for (var u : uriParameters.keySet()) {
			if (!mappedParameterNames.contains(u)) {
				var leftover = uriParameters.get(u);
				collectFixedQuery.accept(new Declaration.FixedQuery(
					leftover.httpName(),
					leftover.value()));
			}
		}
	}

	private List<String> parameterComments(
		DocComment operationComment, Element parameter, String name) {
		DocComment componentComment = datatypes.commentOf(parameter);
		List<String> comment = componentComment.lines();
		if (comment.isEmpty()) {
			comment = operationComment.parameters().getOrDefault(name, List.of());
		}
		return comment;
	}

	private boolean isRequiredParameter(Type type) {
		// currently all containers can be treated as non-required
		// this obviously most applicable to Nullable and Optional,
		// but also empty List/Set
		// This is to be refined in future
		return !(type instanceof Type.Container);
	}

	private List<Declaration.Thrown> extractThrown(
		TypeElement type, ExecutableElement method, ExecutableType mirror,
		DocComment operationComment) {

		var thrown = new ArrayList<Declaration.Thrown>();

		for (var thrownMirror : mirror.getThrownTypes()) {
			// KnownAnnotations.Empty is used because we don't transfer
			// any annotations' qualities from method element itself to it's exceptions
			var errorType = datatypes.interpretType(type, thrownMirror, method,
				KnownAnnotations.Empty);

			int status = extractStatusCode(thrownMirror, DEFAULT_EXCEPTION_STATUS);
			// default for exception

			var bodyType = tryExtractBodyType(thrownMirror)
				.map(bodyMirror -> datatypes.interpretType(type, bodyMirror, method,
					KnownAnnotations.Empty));

			var comment = extractThrownComments(operationComment, thrownMirror);

			thrown.add(new Declaration.Thrown(errorType, thrownMirror, status, bodyType, comment));
		}

		return List.copyOf(thrown);
	}

	private List<String> extractThrownComments(DocComment operationComment, TypeMirror thrownMirror) {
		String fullTypeString = thrownMirror.toString();
		@Null var comment = operationComment.thrown().get(fullTypeString);
		if (comment != null) return comment;
		@Null var element = (TypeElement) types.asElement(thrownMirror);
		if (element != null) {
			comment = operationComment.thrown().get(element.getSimpleName().toString());
			if (comment != null) return comment;
			// just in case, this should never give result as we've tried fullTypeString already
			comment = operationComment.thrown().get(element.getQualifiedName().toString());
			if (comment != null) return comment;
		}
		return List.of(); // give up now, default is empty comment
	}

	private Optional<TypeMirror> tryExtractBodyType(TypeMirror exceptionType) {
		// all these casts are bases on the invariants of the Java language
		// and java annotation processing API
		// for example, exceptionType can always be represented as TypeElement
		// implemented type will be an interface Type element, generic parameter of which
		// we can extract etc.
		for (var implemented : ((TypeElement) types.asElement(exceptionType)).getInterfaces()) {
			var element = (TypeElement) types.asElement(implemented);
			if (element.getQualifiedName().contentEquals(TYPE_RETURNS)) {
				var mirror = ((DeclaredType) implemented).getTypeArguments().get(0);
				return Optional.of(mirror);
			}
		}
		return Optional.empty();
	}

	private Declaration.Return extractReturn(
		TypeElement type,
		ExecutableElement method,
		ExecutableType mirror,
		DocComment operationComment) {

		var returnType = mirror.getReturnType();
		// HTTP_STATUS_OK - default for the return value
		int status = extractStatusCode(returnType, HTTP_STATUS_OK);

		var decodedType = datatypes.interpretType(type, returnType, method);
		return new Declaration.Return(decodedType, returnType, status, operationComment.returns());
	}

	private int extractStatusCode(TypeMirror type, int defaultStatus) {
		// start with type_use annotation, no caching for TypeMirror
		@Null Status status = KnownAnnotations.from(type).get(Status.class);

		// then try annotation on the type itself
		if (status == null) {
			// can be primitive / void - no element available
			@Null var element = types.asElement(type);
			if (element != null) {
				status = knownAnnotationsOf(element).get(Status.class);
			}
		}

		return status != null ? status.value() : defaultStatus;
	}

	private record HttpBinding(PathTemplate template, Declaration.HttpMethod method) {}

	private HttpBinding extractHttpBinding(
		KnownAnnotations annotations, ExecutableElement method, String pathPrefix) {

		@Null Declaration.HttpMethod httpMethod = null;
		@Null PathTemplate template = null;

		for (var presentType : annotations.present()) {
			if (KnownAnnotations.httpMethods.contains(presentType)) {
				var simpleName = presentType.getSimpleName();

				if (httpMethod != null) {
					error("Multiple HTTP method annotations are not allowed: %s, but already was %s"
						.formatted(simpleName, httpMethod.name()), method);
					continue;
				}

				@Null var a = annotations.get(presentType);
				String path;
				// cannot use --enable-preview from annotation processor (Gradle?)
				if (a instanceof GET m) path = m.value();
				else if (a instanceof PUT m) path = m.value();
				else if (a instanceof POST m) path = m.value();
				else if (a instanceof PATCH m) path = m.value();
				else if (a instanceof DELETE m) path = m.value();
				else if (a instanceof OPTIONS m) path = m.value();
				else path = "";

				template = PathTemplate.from(pathPrefix + path);
				httpMethod = Declaration.HttpMethod.valueOf(simpleName);
			}
		}

		if (httpMethod == null) {
			error(("No HTTP method annotation is found on '%s'. " +
					"Use one of @GET, @POST, @PUT etc.").formatted(method.getSimpleName()),
				method);
			// This bogus method value is just to continue processing after error was reported
			// model will be considered invalid anyway
			httpMethod = Declaration.HttpMethod.GET;
			template = PathTemplate.from(pathPrefix);
		}

		return new HttpBinding(template, httpMethod);
	}

	private static final String TYPE_RETURNS = "dev.declaration.http.Returns";

	public static final int HTTP_STATUS_OK = 200;
	public static final int DEFAULT_EXCEPTION_STATUS = 500;
}
