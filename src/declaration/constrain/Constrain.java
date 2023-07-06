package io.immutables.declaration.constrain;

import io.immutables.meta.SkippableReturn;

public interface Constrain {
	@SkippableReturn
	static <R extends Record> Constrainer<R> constrain(R record) {
		return new Constrainer<R>() {
			@Override
			public Constrainer<R> require(boolean condition) {
				return null;
			}

			@Override
			public <T> Checker<T, R> require(T value) {
				return new Checker<>() {

					@Override
					public Constrainer<R> inRange(int min, int max) {
						return null;
					}

					@Override
					public Constrainer<R> matches(String regex) {
						return null;
					}
				};
			}
		};
	}

	@SkippableReturn
	static <R extends Record> Constrainer<R> constrain(R record, Runnable r) {
		throw new UnsupportedOperationException();
	}

	interface Constrainer<R> {
		Constrainer<R> require(boolean condition);
		<T> Checker<T, R> require(T value);
	}

	interface Checker<T, R> {

		Constrainer<R> inRange(int min, int max);

		Constrainer<R> matches(String regex);
	}
}
