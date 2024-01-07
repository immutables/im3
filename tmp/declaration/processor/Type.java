package io.immutables.declaration.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.type.TypeMirror;

public sealed interface Type {

	enum Primitive implements Type {
		Null,
		Boolean,
		Integer,
		Long,
		Float,
		String,
		Void
	}

	record Variable(int variable, String name) implements Type {}

	record Terminal(
		Declaration.Reference terminal
	) implements Type {}

	record Container(Kind container, Type element) implements Type {

		public enum Kind {
			Nullable,
			Optional,
			// Temporary hack before we find out the way to align
			// OptionalInt and Optional<Integer> :: Optional<T> where T : Integer
			OptionalPrimitive,
			List,
			Set,
		}
	}

	record Applied(
		Declaration.Reference applies,
		List<Type> arguments
	) implements Type {}

	record Mirror(TypeMirror mirror) implements Type {
		public String toString() {
			return mirror.toString();
		}
	}
}
