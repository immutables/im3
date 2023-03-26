package io.immutables.declaration.processor;

import io.immutables.common.Vect;

sealed interface Type {
	enum Primitive implements Type {
		Null,
		Boolean,
		Integer,
		Long,
		Float,
		String
	}

	/** This is made to have an object identity, no equals / hashCode - on purpose. */
	final class Variable implements Type {
		private final String name;

		Variable(String name) {
			this.name = name;
		}
	}

	record Terminal(
		Declaration declaration
	) implements Type {

		public Terminal {
			assert declaration.parameters().isEmpty();
		}
	}

	record Applied(
		Declaration declaration,
		Vect<Type> arguments
	) implements Type {

		public Applied {
			assert !arguments.isEmpty();
			assert declaration.parameters().size() == arguments.size();
		}
	}
}
