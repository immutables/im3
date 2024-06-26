package io.immutables.lang.test;

import io.immutables.lang.node.Identifier;
import io.immutables.lang.typeold2.Type;
import io.immutables.lang.typeold2.TypeConstructor;
import io.immutables.lang.typeold2.TypeSignature;

interface FixtureTypes {
	Identifier.Pool p = new Identifier.Pool();

	Type.Terminal i32 = Type.Terminal.define(p.id("i32"));
	Type.Terminal bool = Type.Terminal.define(p.id("bool"));
	Type.Terminal str = Type.Terminal.define(p.id("str"));

	Type.Variable J = Type.Variable.allocate(p.id("J"));
	Type.Variable K = Type.Variable.allocate(p.id("K"));
	Type.Variable L = Type.Variable.allocate(p.id("L"));

	Type.Variable X = Type.Variable.allocate(p.id("X"));
	Type.Variable Y = Type.Variable.allocate(p.id("Y"));
	Type.Variable Z = Type.Variable.allocate(p.id("Z"));

	Type.Product Empty = Type.Product.Empty;

	TypeConstructor Aa_T = TypeSignature.name(p.id("Aa"), p.id("T")).constructor();
	TypeConstructor Bb_W_Y = TypeSignature.name(p.id("Bb"), p.id("W"), p.id("Y")).constructor();

	static Type.Product productOf(Type c0, Type c1, Type... components) {
		return Type.Product.of(c0, c1, components);
	}
}
