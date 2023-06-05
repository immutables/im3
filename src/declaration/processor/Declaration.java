package io.immutables.declaration.processor;

import io.immutables.meta.Null;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// cannot be nested into declaration
interface Documented {
	String comment();
}

/**
 * Declaration represent type as defined, not how it's used or referenced.
 * These are only for nominal types, user-defined and maybe some synthetic)
 * and not for builtin primitives.
 */
public sealed interface Declaration extends Documented {
	Reference reference();

	default String name() {
		return reference().name();
	}

	default String module() {
		return reference().module();
	}

	interface Parameterizable {
		List<Type.Variable> parameters();
	}

	record Inline(
		Tag inline,
		Reference reference,
		List<Type.Variable> parameters,
		Component component,
		String comment
	) implements Declaration, Parameterizable {
		public enum Tag {Is}
	}

	record Product(
		Tag product,
		Reference reference,
		List<Type.Variable> parameters,
		List<Component> components,
		String comment
	) implements Declaration, Parameterizable {
		public enum Tag {Is}
		public Product {
			assert components.size() >= 2;
		}
	}

	record Enum(
		Tag enums,
		Reference reference,
		List<Constant> constants,
		String comment) implements Declaration {
		public enum Tag {Is}
		record Constant(String name) {}
	}

	record Record(
		Tag product,
		Reference reference,
		List<Type.Variable> parameters,
		List<Component> components,
		String comment
	) implements Declaration, Parameterizable {
		public enum Tag {Is}
	}

	record Sealed(
		Tag sealed,
		Reference reference,
		List<Type.Variable> parameters,
		List<Declaration> cases,
		String comment
	) implements Declaration, Parameterizable {
		public enum Tag {Is}
	}

	record Component(String name, Type type, String javaType, String comment)
		implements Documented {}

	record Contract(
		Tag contract,
		Reference reference,
		String pathPrefix,
		Map<String, Operation> operations,
		String comment
	) implements Declaration {
		public enum Tag {Is}
	}

	record Operation(
		String name,
		PathTemplate path,
		HttpMethod method,
		Return returns,
		List<Thrown> thrown,
		List<Parameter> parameters,
		List<FixedQuery> fixedQuery,
		String comment
	) implements Documented {}

	record FixedQuery(String name, @Null String value) {}

	record Return(
		Type type,
		int status
	) {}

	record Thrown(
		Type type,
		int status,
		Optional<Type> bodyType
	) {}

	record Parameter(
		String name,
		String httpName,
		int index,
		Type type,
		Mapping mapping,
		String comment
	) implements Documented {
		public enum Mapping {
			Path,
			Query,
			Body, Unmapped,
			// Header, Matrix // Who needs these?
		}
	}

	// These are uppercase to mimic HTTPs method spelling
	enum HttpMethod {
		GET,
		PUT,
		POST,
		DELETE,
		PATCH,
		OPTIONS
	}

	/**
	 * Symbolic reference to a Declaration, which needs to be resolved/de-referenced
	 * to a real declaration. We use it to marshall and pass declarations around by-reference.
	 */
	record Reference(
		String module,
		String name) {}
}
