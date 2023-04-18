package io.immutables.that;

import io.immutables.meta.Null;
import java.util.Arrays;
import java.util.stream.Collectors;
/**
 * {@code Assert.that}: Minimalistic, extensible and attentive to details assertions.
 */
public final class Assert {
	private Assert() {}

	/**
	 * @return that condition
	 */
	public static That.Condition that() {
		class Tested implements That.Condition {}
		return new Tested();
	}

	/**
	 * @param actualRuns runnable or lambda
	 * @return that runnable
	 */
	public static That.Runnable that(Runnable actualRuns) {
		class Tested extends That.What<Runnable, That.Runnable> implements That.Runnable {}
		return new Tested().set(actualRuns);
	}

	/**
	 * @param <T> actual object type
	 * @param actual object
	 * @return that object
	 */
	public static <T> That.Object<T> that(@Null T actual) {
		class Tested extends That.What<T, That.Object<T>> implements That.Object<T> {}
		return new Tested().set(actual);
	}

	/**
	 * @param <T> actual object type
	 * @param actual optional object
	 * @return that optional
	 */
	public static <T> That.Optional<T> that(@Null java.util.Optional<T> actual) {
		class Tested extends That.What<java.util.Optional<T>, That.Optional<T>> implements That.Optional<T> {}
		return new Tested().set(actual);
	}

	/**
	 * @param <T> actual element type
	 * @param actual iterable object
	 * @return that iterable
	 */
	public static <T> That.Iterable<T> that(@Null Iterable<T> actual) {
		class Tested extends That.What<Iterable<T>, That.Iterable<T>> implements That.Iterable<T> {}
		return new Tested().set(actual);
	}

	/**
	 * @param <T> actual element type
	 * @param actual stream object
	 * @return that iterable
	 */
	public static <T> That.Iterable<T> that(@Null java.util.stream.Stream<T> actual) {
		class Tested extends That.What<Iterable<T>, That.Iterable<T>> implements That.Iterable<T> {}
		return new Tested().set(actual.collect(Collectors.toList()));
	}

	/**
	 * @param <T> actual component type
	 * @param actual array object
	 * @return that iterable
	 */
	@SafeVarargs
	public static <T> That.Iterable<T> that(T... actual) {
		class Tested extends That.What<Iterable<T>, That.Iterable<T>> implements That.Iterable<T> {}
		return new Tested().set(Arrays.asList(actual));
	}

	/**
	 * @param actual long value or null
	 * @return that double
	 */
	public static That.String that(@Null String actual) {
		class Tested extends That.What<String, That.String> implements That.String {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual long value or null
	 * @return that double
	 */
	public static That.Boolean that(@Null Boolean actual) {
		class Tested extends That.What<Boolean, That.Boolean> implements That.Boolean {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual long value or null
	 * @return that double
	 */
	public static That.Double that(@Null Double actual) {
		class Tested extends That.What<Double, That.Double> implements That.Double {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual long value or null
	 * @return that long
	 */
	public static That.Long that(@Null Long actual) {
		class Tested extends That.What<Long, That.Long> implements That.Long {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual int value or null
	 * @return that integer
	 */
	public static That.Int that(@Null Integer actual) {
		class Tested extends That.What<Integer, That.Int> implements That.Int {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual short value or null
	 * @return that integer
	 */
	public static That.Int that(@Null Short actual) {
		return that(actual == null ? null : actual.intValue());
	}

	/**
	 * @param actual character value or null
	 * @return that integer
	 */
	public static That.Int that(@Null Character actual) {
		return that(actual == null ? null : (int) actual);
	}

	/**
	 * @param actual float value or null
	 * @return that double
	 */
	public static That.Double that(@Null Float actual) {
		return that(actual == null ? null : actual.doubleValue());
	}
}
