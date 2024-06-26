package io.immutables.lang.typeold2;

import io.immutables.lang.Vect;

public interface TypeConstructor extends Named, Parameterizable {
	Vect<Type.Parameter> parameters();
	Type.Nominal instantiate(Vect<Type> arguments);

	default Type.Nominal instantiate(Type... arguments) {
		return instantiate(Vect.of(arguments));
	}
}
