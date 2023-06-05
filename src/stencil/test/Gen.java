package io.immutables.stencil.test;

import io.immutables.stencil.Generator;
import io.immutables.stencil.Stencil;

@Generator
public class Gen extends Stencil {
	int nnn() {return 1;}

	boolean ex() {return true;}

	void runi(Runnable r) {}

	C c() {throw new UnsupportedOperationException();}

	interface  C extends Iterable<Integer> { C c(); }
}
