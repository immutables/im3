package dev.declaration.processor;

import java.util.List;

public sealed interface Type {

	enum Primitive implements Type {
		Null,
		Boolean,
		Integer,
		Long,
		Float,
		String,
		Void,
	}

	// TODO move to dynamic registry
	enum Extended implements Type {
		Bytes,
		Uuid,
		Uri,
		Instant,
		LocalDate,
		LocalTime,
		LocalDateTime,
		OffsetDateTime,
		Any,
		MapAny
	}

	record Variable(int variable, String name) implements Type {}

	record Terminal(
		Declaration.Reference terminal
	) implements Type {}

	record Applied(
		Declaration.Reference applies,
		List<Type> arguments
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

		boolean isSerialArray() {
			return switch (container) {
				case List, Set -> true;
				default -> false;
			};
		}

		boolean isSerialOptional() {
			return switch (container) {
				case Nullable, Optional, OptionalPrimitive -> true;
				default -> false;
			};
		}
	}
}
