package io.immutables.codec;

import io.immutables.meta.Null;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;
import static java.util.Objects.requireNonNull;

public final class Types {
	private Types() {}

	/**
	 * Specific types are such types which are either terminal, non-parameterized
	 * types, or, if parameterized, requires all type arguments
	 * also be specific types recursively.
	 * I.e. we ban any type variables and existential types.
	 */
	public static Type requireSpecific(Type type) {
		if (type instanceof Class<?> c) {
			var parameters = c.getTypeParameters();
			if (parameters.length != 0) throw new IllegalArgumentException(
				"a type cannot contain raw types with its arguments missing,"
					+ "use fully instantiated type " + c);
		} else if (type instanceof ParameterizedType p) {
			for (var a : p.getActualTypeArguments()) {
				requireSpecific(a);
			}
		} else throw new IllegalArgumentException(
			"a type cannot contain non-specific types. But contained " + type);

		return rewrapParameterized(type);
	}

	public static Type[] requireSpecific(Type[] types) {
		types = types.clone();
		for (int i = 0; i < types.length; i++) {
			types[i] = requireSpecific(types[i]);
		}
		return types;
	}

	public static ParameterizedType newParameterized(Class<?> raw, Type... arguments) {
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

	public static Type[] getArguments(Type type) {
		return type instanceof ParameterizedType p ? p.getActualTypeArguments() : new Type[0];
	}

	public static Type getFirstArgument(Type type) {
		return ((ParameterizedType) type).getActualTypeArguments()[0];
	}

	public static Class<?> toRawType(Type type) {
		return switch (type) {
			case Class<?> c -> c;
			case ParameterizedType p -> (Class<?>) p.getRawType();
			case TypeVariable<?> v -> toRawType(v.getBounds()[0]);
			case WildcardType w -> toRawType(w.getUpperBounds()[0]);
			default -> throw new IllegalArgumentException("No raw type for " + type);
		};
	}

	public static Map<TypeVariable<?>, Type> mapArgumentsInHierarchy(Class<?> raw) {
		return mapArgumentsInHierarchy(raw, Map.of());
	}

	public static Map<TypeVariable<?>, Type> mapArgumentsInHierarchy(
		Class<?> raw, Map<? extends TypeVariable<?>, ? extends Type> initial) {
		var resolution = new HierarchyTypeVariableMapper();
		resolution.variables.putAll(initial);
		resolution.collect(raw);
		return resolution.variables;
	}

	public static Map<TypeVariable<?>, Type> mapArguments(Class<?> raw, Type type) {
		if (type instanceof ParameterizedType p) {
			var arguments = p.getActualTypeArguments();
			var parameters = raw.getTypeParameters();

			assert p.getRawType().equals(raw);
			assert parameters.length == arguments.length;

			var map = new HashMap<TypeVariable<?>, Type>(4);
			for (int i = 0; i < arguments.length; i++) {
				map.put(parameters[i], arguments[i]);
			}

			return map; // frankly, no need to wrap in immutable map
		} else if (raw.equals(type)) {
			assert raw.getTypeParameters().length == 0;
			return Map.of();
		} else throw new IllegalArgumentException("Unsupported type kind: " + type);
	}

	private static final class AppliedType implements ParameterizedType {
		final Class<?> raw;
		final Type[] arguments;

		private AppliedType(Class<?> raw, Type[] arguments) {
			this.raw = requireNonNull(raw);
			this.arguments = requireNonNull(arguments);
		}

		public Type[] getActualTypeArguments() {
			return arguments.clone();
		}

		public Type getRawType() {
			return raw;
		}

		public @Null Type getOwnerType() {
			return null;
		}

		public Class<?> raw() {return raw;}

		public Type[] arguments() {return arguments;}

		@Override
		public String toString() {
			var a = Arrays.toString(arguments);// then cut off square brackets
			return raw.getName() + '<' + a.substring(0, a.length() - 1) + '>';
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || (obj instanceof AppliedType a
				&& raw == a.raw
				&& Arrays.equals(arguments, a.arguments));
		}

		@Override
		public int hashCode() {
			return Objects.hash((Object[]) arguments) * 31 + raw.hashCode();
		}
	}

	public static Type[] resolveArguments(Type[] types, Map<TypeVariable<?>, Type> variables) {
		types = types.clone();
		for (int i = 0; i < types.length; i++) {
			types[i] = resolveArguments(types[i], variables);
		}
		return types;
	}

	public static Type resolveArguments(Type type, Map<TypeVariable<?>, Type> variables) {
		if (type instanceof Class<?>) return type;

		if (type instanceof ParameterizedType p) {
			// this array is always expected as a clone, never original
			Type[] arguments = p.getActualTypeArguments();
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = resolveArguments(arguments[i], variables);
			}
			return new AppliedType((Class<?>) p.getRawType(), arguments);
		}

		if (type instanceof TypeVariable<?> v) {
			@Null Type substitution = variables.get(v);
			if (substitution == null) throw new IllegalArgumentException(
				("Must have all variables substituted! Missing %s occurs in %s " +
					"where existing substitutions: %s").formatted(v, type.getTypeName(), variables));

			if (assertionsEnabled) try {
				requireSpecific(substitution);
			} catch (Exception e) {
				throw new AssertionError(e);
			}

			return substitution;
		}
		throw new IllegalArgumentException("Unsupported type: " + type);
		// TODO not sure if it's a good idea, need to see bigger picture
	}

	// This is limited only to extracting type resolution started with fully specific
	private static final class HierarchyTypeVariableMapper {
		final Map<TypeVariable<?>, Type> variables = new HashMap<>();
		private final Set<Class<?>> seen = new HashSet<>();

		void collect(Class<?> raw) {
			collect(raw, raw);
		}

		void collect(Class<?> raw, Type type) {
			// just to make it more resilient for future changes
			if (raw == Object.class) return;

			if (seen.add(raw)) {
				var arguments = mapArguments(raw, type);
				variables.putAll(arguments);

				@Null Type superClass = raw.getGenericSuperclass();
				if (superClass != null) {
					collect(toRawType(superClass),
						resolveArguments(superClass, variables));
				}

				for (Type superInterface : raw.getGenericInterfaces()) {
					collect(toRawType(superInterface),
						resolveArguments(superInterface, variables));
				}
			} else if (assertionsEnabled) {
				// if assertions enabled we just recheck that resolved variables
				// table contains the same resolved arguments for the type definition
				for (var e : mapArguments(raw, type).entrySet()) {
					var variable = e.getKey();
					var argument = e.getValue();
					assert argument.equals(variables.get(variable));
				}
			}
		}
	}

	private static final boolean assertionsEnabled = Types.class.desiredAssertionStatus();
}
