package io.immutables.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

final class Types {
	private Types() {}

	/**
	 * Specific types are such types which are either terminal, non-parameterized
	 * types, or, if parameterized, requires all type arguments
	 * also be specific types recursively.
	 * I.e. we ban any type variables and existential types.
	 */
	static void requireSpecificTypes(Type type) {
		if (type instanceof Class<?> c) {
			var parameters = c.getTypeParameters();
			if (parameters.length != 0) throw new IllegalArgumentException(
				"a type cannot contain raw types with its arguments missing,"
					+ "use fully instantiated type " + c);
		} else if (type instanceof ParameterizedType p) {
			for (var a : p.getActualTypeArguments()) {
				requireSpecificTypes(a);
			}
		} else throw new IllegalArgumentException(
			"a type cannot contain non-specific types. But contained " + type);
	}
}
