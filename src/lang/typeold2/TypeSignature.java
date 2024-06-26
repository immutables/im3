package io.immutables.lang.typeold2;

import io.immutables.lang.Vect;
import io.immutables.lang.node.Identifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class TypeSignature implements Named, Parameterizable {
	private final Identifier name;
	private final Vect<Type.Parameter> parameters;
	private final TypeConstructor constructor;

	private TypeSignature(Identifier name, Vect<Type.Parameter> parameters) {
		this.name = name;
		this.parameters = parameters;
		this.constructor = parameters.isEmpty()
				? Type.Terminal.define(name)
				: parameterizedConstructor();
	}

	private TypeConstructor parameterizedConstructor() {
		return new TypeConstructor() {
			@Override public Identifier name() {
				return name;
			}

			@Override public Vect<Type.Parameter> parameters() {
				return parameters;
			}

			@Override public Type.Nominal instantiate(Vect<Type> arguments) {
				return Type.Applied.instance(this, arguments);
			}

			@Override public String toString() {
				return name + parameters.join(", ", "<", ">");
			}
		};
	}

	@Override public Identifier name() {
		return name;
	}

	@Override public Vect<Type.Parameter> parameters() {
		return parameters;
	}

	public TypeConstructor constructor() {
		return constructor;
	}

	public static Composer compose(Identifier name) {
		return new Composer(name);
	}

	@Override public String toString() {
		return constructor.toString();
	}

	public static TypeSignature name(Identifier name, Identifier... parameters) {
		var c = new Composer(name);
		for (var p : parameters) c.parameter(p);
		return c.create();
	}

	public static final class Composer {
		private final Identifier name;
		private final Map<Identifier, Type.Parameter> parametersByName = new IdentityHashMap<>();
		private final List<Type.Parameter> parameters = new ArrayList<>();

		private Composer(Identifier name) {
			this.name = name;
		}

		public Type.Parameter parameter(Identifier name) {
			var parameter = Type.Parameter.introduce(name, parametersByName.size());
			var previous = parametersByName.put(name, parameter);
			if (previous != null) throw new IllegalStateException(
				"duplicate parameter in signature " + name);
			parameters.add(parameter);
			return parameter;
		}

		public TypeSignature create() {
			return new TypeSignature(name, Vect.from(parameters));
		}
	}
}
