package io.immutables.stencil.test;

import io.immutables.stencil.Generator;
import io.immutables.stencil.Stencil;
import java.lang.annotation.RetentionPolicy;

@Generator
public class Gen extends Stencil {
	void a() {}

	int nnn() {return 1;}

	boolean ex() {return true;}

	void runi(Runnable r) {}

	C c() {throw new UnsupportedOperationException();}

	interface C extends Iterable<Integer> {
		C c();
	}

	RetentionPolicy retention() {
		return switch ((int) (Math.floor(Math.random() * 3))) {
			case 0 -> RetentionPolicy.SOURCE;
			case 1 -> RetentionPolicy.CLASS;
			default -> RetentionPolicy.RUNTIME;
		};
	}
}
