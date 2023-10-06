package io.immutables.codec;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public sealed interface Problem {
	record MissingField(String at, String field) implements Problem {}
	record UnknownField(String at, String field) implements Problem {}
	record CannotInstantiate(String at, String type) implements Problem {}
	record UnexpectedType(String at, String expected, String actual) implements Problem {}
	record NoMatchingCase(String at, String type) implements Problem {}

	abstract class Handler {
		public abstract void enque(Problem problem) throws IOException;
		public abstract List<Problem> hasProblems();
		public abstract boolean overflowed();
	}
}
