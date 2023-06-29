package io.immutables.declaration.processor;

import io.immutables.declaration.http.*;
import io.immutables.meta.Null;
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

		return Optional.of(new Declaration.Contract(
			Declaration.Contract.Tag.Is,
			datatypes.reference(type),
			pathPrefix,
			extractOperations(type, pathPrefix),
			commentOf(type)
		));
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

			// Maybe warn?
			if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.NATIVE)) continue;

			var violations = new ArrayList<Runnable>();

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

			var returns = extractReturn(method, mirror);
			var thrown = extractThrown(mirror);

			var collectedParameters = new ArrayList<Declaration.Parameter>();
			var collectedFixedQuery = new ArrayList<Declaration.FixedQuery>();

			collectParameters(
				method, mirror, binding,
				collectedParameters::add, collectedFixedQuery::add);

			operations.put(name, new Declaration.Operation(
				name,
				binding.template,
				binding.method,
				returns,
				thrown,
				List.copyOf(collectedParameters),
				List.copyOf(collectedFixedQuery),
				commentOf(method)
			));
		}

		return Collections.unmodifiableMap(operations);
	}

	private void collectParameters(
		ExecutableElement method, ExecutableType mirror, HttpBinding binding,
		Consumer<Declaration.Parameter> collectParameters,
		Consumer<Declaration.FixedQuery> collectFixedQuery) {

		// here we don't check if, for example, request entity is compatible with certain HTTP
		// method.
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

			var type = new Type.Mirror(parameterTypes.get(index));
			collectParameters.accept(new Declaration.Parameter(
				name, httpName, index, type, mapping,
				commentOf(p)));
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

	private List<Declaration.Thrown> extractThrown(ExecutableType mirror) {
		var thrown = new ArrayList<Declaration.Thrown>();

		for (var t : mirror.getThrownTypes()) {
			var type = new Type.Mirror(t);
			int status = extractStatusCode(t, 500); // default for exception
			var bodyType = tryExtractBodyType(t);

			thrown.add(new Declaration.Thrown(type, status, bodyType));
		}

		return List.copyOf(thrown);
	}

	private Optional<Type> tryExtractBodyType(TypeMirror exceptionType) {
		// all these casts are bases on the invariants of the Java language
		// and java annotation processing API
		// for example, exceptionType can always be represented as TypeElement
		// implemented type will be an interface Type element, generic parameter of which
		// we can extract etc.
		for (var implemented : ((TypeElement) types.asElement(exceptionType)).getInterfaces()) {
			var element = (TypeElement) types.asElement(implemented);
			if (element.getQualifiedName().contentEquals(TYPE_RETURNS)) {
				var mirror = ((DeclaredType) implemented).getTypeArguments().get(0);
				return Optional.of(new Type.Mirror(mirror));
			}
		}
		return Optional.empty();
	}

	private Declaration.Return extractReturn(ExecutableElement method, ExecutableType mirror) {
		var returnType = mirror.getReturnType();
		int status = extractStatusCode(returnType, 200); // default for the return value

		return new Declaration.Return(new Type.Mirror(returnType), status);
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
			error("No HTTP method annotation is found on '%s'".formatted(method.getSimpleName()),
				method);
			// This bogus method value is just to continue processing after error was reported
			// model will be considered invalid anyway
			httpMethod = Declaration.HttpMethod.GET;
			template = PathTemplate.from(pathPrefix);
		}

		return new HttpBinding(template, httpMethod);
	}

	private String commentOf(Element element) {
		return datatypes.commentOf(element);
	}

	public static final String TYPE_RETURNS = "io.immutables.declaration.http.Returns";
}
