package io.immutables.declaration.processor;

import io.immutables.common.Vect;

/**
 * Declaration represent type as defined, not how it's used or referenced.
 * These are only for nominal types, user-defined and maybe some synthetic)
 * and not for builtin primitives.
 */
interface Declaration {
	record Inline(
		String name,
		Component component,
		Vect<Type.Variable> parameters
	) implements Declaration {

	}

	record Product(
		String name,
		Vect<Component> components,
		Vect<Type.Variable> parameters
	) implements Declaration {
		public Product {
			assert components.size() >= 2;
		}
	}

	record Enum(String name, Vect<String> constants) {}

	record Record(
		String name,
		Vect<Type.Variable> parameters,
		Vect<Component> components
	) implements Declaration {}

	record Sealed(String name, Vect<Declaration> cases) implements Declaration {}

	default Vect<Type.Variable> parameters() {
		return Vect.of();
	}

	record Component(String name, Type type) {}

	record Contract(
		String name,
		Vect<Operation> operations,
		Path path
	) {}

	record Operation(
		String name,
		Type returnType,
		Vect<Parameter> parameters,
		Path path
	) {}

	record Parameter(
		String name,
		Type type,
		Mapping mapping
	) {
		enum Mapping {
			Path,
			Query,
			Entity,
			// Header, Matrix // Who needs these?
		}
	}
}
