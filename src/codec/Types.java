package io.immutables.codec;

import io.immutables.meta.Null;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

public final class Types {
	private Types() {}

	/**
	 * Specific types are such types which are either terminal, non-parameterized
	 * types, or, if parameterized, requires all type arguments
	 * also be specific types recursively.
	 * I.e. we ban any type variables and existential types.
	 */
	public static Type requireSpecificTypes(Type type) {
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

		return rewrapParameterized(type);
	}

	public static ParameterizedType newParameterizedType(Class<?> raw, Type... arguments) {
		if (arguments.length != raw.getTypeParameters().length) {
			throw new IllegalArgumentException("Arguments length and parameters mismatch");
		}
		return new AppliedType(raw, rewrapParameterized(arguments));
	}

	private static Type[] rewrapParameterized(Type[] arguments) {
		arguments = arguments.clone();
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = rewrapParameterized(arguments[i]);
		}
		return arguments;
	}

	/**
	 * May be useful in assertions. Implemented as a check if we rewrap(possibly again)
	 * we would get exactly the same instance
	 */
	public static boolean isRewrapped(Type type) {
		return type == rewrapParameterized(type);
	}

	private static Type rewrapParameterized(Type argument) {
		if (argument instanceof ParameterizedType p) {
			if (p instanceof AppliedType) return p;
			return new AppliedType(
				(Class<?>) p.getRawType(),
				rewrapParameterized(p.getActualTypeArguments()));
		}
		return argument;
	}

	static Type getFirstArgument(Type type) {
		return ((ParameterizedType) type).getActualTypeArguments()[0];
	}

	private record AppliedType(Class<?> raw, Type[] arguments)
		implements ParameterizedType {

		public Type[] getActualTypeArguments() {
			return arguments;
		}

		public Type getRawType() {
			return raw;
		}

		public @Null Type getOwnerType() {
			return null;
		}
	}

	public static Type resolveTypeArguments(Type type, Map<TypeVariable<?>, Type> variables) {
		if (type instanceof Class<?>) {
			return type;
		}
		if (type instanceof ParameterizedType p) {
			// this array is always expected as a clone, never original
			Type[] arguments = p.getActualTypeArguments();
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = resolveTypeArguments(arguments[i], variables);
			}
			return new AppliedType((Class<?>) p.getRawType(), arguments);
		}
		if (type instanceof TypeVariable<?> v) {
			@Null Type substitution = variables.get(v);
			if (substitution == null) throw new IllegalArgumentException(
				"Must have all variables substituted! Missing %s occurring in %s where substitutions are %s".formatted(v, type, variables));

			if (assertionsEnabled) try {
				requireSpecificTypes(substitution);
			} catch (Exception e) {
				throw new AssertionError(e);
			}

			return substitution;
		}
		throw new IllegalArgumentException("Unsupported type: " + type);
		// TODO not sure if it's a good idea, need to see bigger picture
	}

	private static final boolean assertionsEnabled = Types.class.desiredAssertionStatus();
}