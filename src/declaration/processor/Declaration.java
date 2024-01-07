package dev.declaration.processor;

import io.immutables.meta.Null;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

/**
 * Declaration represent type as defined, not how it's used or referenced.
 * These are only for nominal types, user-defined (and maybe some synthetic)
 * and not for builtin primitives.
 */
public sealed interface Declaration extends Documented {
	// TODO move to the top level, rename?
	record Module(
		String name,
		List<Declaration> declarations,
		List<String> comment
	) implements Documented {}

	/**
	 * Symbolic reference to a Declaration, which needs to be resolved/de-referenced
	 * to a real declaration. We use it to marshall and pass declarations around by-reference.
	 */
	// TODO Move to top level
	record Reference(
		String module,
		String name) {}

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

	// TODO extract to top level and nest datatypes there
	// Do this before doing switch with pattern matching
	sealed interface Datatype extends Declaration {}

	record Inline(
		Tag inline,
		Reference reference,
		List<Type.Variable> parameters,
		Component component,
		List<String> comment
	) implements Datatype, Parameterizable {
		public enum Tag { Is }
	}

	record Product(
		Tag product,
		Reference reference,
		List<Type.Variable> parameters,
		List<Component> components,
		List<String> comment
	) implements Datatype, Parameterizable {
		public enum Tag { Is }

		public Product {
			assert components.size() >= 2;
		}
	}

	record Enum(
		Tag enums,
		Reference reference,
		List<Constant> constants,
		List<String> comment
	) implements Datatype {
		public enum Tag { Is }

		record Constant(String name, List<String> comment) implements Documented {}
	}

	record Record(
		Tag product,
		Reference reference,
		List<Type.Variable> parameters,
		List<Component> components,
		List<String> comment
	) implements Datatype, Parameterizable {
		public enum Tag { Is }

		boolean hasRequired() {
			return components.stream().anyMatch(Component::required);
		}
	}

	record Sealed(
		Tag sealed,
		Reference reference,
		List<Type.Variable> parameters,
		List<Datatype> cases,
		List<String> comment
	) implements Datatype, Parameterizable {
		public enum Tag { Is }
	}

	record Component(String name, Type type, String javaType, List<String> comment)
		implements Documented {
		/**
		 * If component can be omitted, but a record can still be constructed
		 * with some default value, usually {@code null}
		 */
		boolean required() {
			return type != Type.Primitive.Null
				&& type != Type.Primitive.Void
				&& !(type instanceof Type.Container);
			// We don't have a mechanism yet to require array hardly (except for validation)
		}
	}

	record Contract(
		Tag contract,
		Reference reference,
		String pathPrefix,
		Map<String, Operation> operations,
		List<String> comment
	) implements Declaration {
		public enum Tag { Is }
	}

	record Operation(
		String name,
		PathTemplate path,
		HttpMethod method,
		Return returns,
		List<Thrown> thrown,
		List<Parameter> parameters,
		List<FixedQuery> fixedQuery,
		List<String> comment
	) implements Documented {}

	record FixedQuery(String name, @Null String value) {}

	record Return(
		Type type,
		TypeMirror javaType,
		int status,
		List<String> comment
	) implements Documented {}

	record Thrown(
		Type type,
		TypeMirror javaType,
		int status,
		Optional<Type> bodyType,
		List<String> comment
	) implements Documented {}

	record Parameter(
		String name,
		String httpName,
		int index,
		Type type,
		TypeMirror javaType,
		Mapping mapping,
		boolean required,
		List<String> comment
	) implements Documented {
		public enum Mapping {
			Path,
			Query,
			Body,
			Unmapped,
			// Header, Matrix // Who needs these?
		}
	}

	// These are all-uppercase to mimic HTTPs method spelling
	enum HttpMethod {
		GET,
		PUT,
		POST,
		DELETE,
		PATCH,
		OPTIONS
	}
}
