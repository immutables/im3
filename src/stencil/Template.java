package io.immutables.stencil;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Extend generators from template to get a set of handy protected methods,
 * general purpose and useful for typical programming language generation
 */
public abstract class Template extends Stencil {

	protected void literal(Object value) {
		out().put(Literals.string(stringify(value)));
	}

	protected void print(Object value) {
		System.err.print(stringify(value));
	}

	protected static Iterable<Integer> range(int to) {
		return IntStream.range(0, to).boxed()::iterator;
	}

	protected static Iterable<Integer> range(int from, int to) {
		return IntStream.range(from, to).boxed()::iterator;
	}

	protected static List<String> lines(String text) {
		return List.of(text.split("\\n"));
	}
}
