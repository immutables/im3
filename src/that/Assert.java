package io.immutables.that;

import io.immutables.meta.Null;
import java.util.Arrays;
import java.util.stream.Collectors;
/**
 * {@code Assert.that}: Minimalistic, extensible and attentive-to-details assertions.
 */
public final class Assert {
	private Assert() {}

	/**
	 * @return that condition
	 */
	public static That.AssertedCondition that() {
		class Tested implements That.AssertedCondition {}
		return new Tested();
	}

	/**
	 * @param actualRuns runnable or lambda
	 * @return that runnable
	 */
	public static That.AssertedRunnable that(CanThrow actualRuns) {
		class Tested extends That.What<CanThrow, That.AssertedRunnable>
				implements That.AssertedRunnable {}
		return new Tested().set(actualRuns);
	}

	public interface CanThrow {
		void run() throws Exception;
	}

	/**
	 * @param <T> actual object type
	 * @param actual object
	 * @return that object
	 */
	public static <T> That.AssertedObject<T> that(@Null T actual) {
		class Tested extends That.What<T, That.AssertedObject<T>> implements That.AssertedObject<T> {}
		return new Tested().set(actual);
	}

	/**
	 * @param <T> actual object type
	 * @param actual optional object
	 * @return that optional
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static <T> That.AssertedOptional<T> that(@Null java.util.Optional<T> actual) {
		class Tested extends That.What<java.util.Optional<T>, That.AssertedOptional<T>>
				implements That.AssertedOptional<T> {}
		return new Tested().set(actual);
	}

	/**
	 * @param <T> actual element type
	 * @param actual iterable object
	 * @return that iterable
	 */
	public static <T> That.AssertedIterable<T> that(@Null Iterable<T> actual) {
		class Tested extends That.What<Iterable<T>, That.AssertedIterable<T>>
				implements That.AssertedIterable<T> {}
		return new Tested().set(actual);
	}

	/**
	 * @param <T> actual element type
	 * @param actual stream object
	 * @return that iterable
	 */
	public static <T> That.AssertedIterable<T> that(@Null java.util.stream.Stream<T> actual) {
		class Tested extends That.What<Iterable<T>, That.AssertedIterable<T>>
				implements That.AssertedIterable<T> {}
		return new Tested().set(actual == null ? null : actual::iterator);
	}

	/**
	 * @param <T> actual component type
	 * @param actual array object
	 * @return that iterable
	 */
	@SafeVarargs
	public static <T> That.AssertedIterable<T> that(T... actual) {
		class Tested extends That.What<Iterable<T>, That.AssertedIterable<T>>
				implements That.AssertedIterable<T> {}
		return new Tested().set(Arrays.asList(actual));
	}

	/**
	 * @param actual array object
	 * @return that iterable
	 */
	public static That.AssertedIterable<Integer> that(int[] actual) {
		class Tested extends That.What<Iterable<Integer>, That.AssertedIterable<Integer>>
				implements That.AssertedIterable<Integer> {}
		return new Tested().set(Arrays.stream(actual).boxed().toList());
	}

	/**
	 * @param actual array object
	 * @return that iterable
	 */
	public static That.AssertedIterable<Long> that(long[] actual) {
		class Tested extends That.What<Iterable<Long>, That.AssertedIterable<Long>>
				implements That.AssertedIterable<Long> {}
		return new Tested().set(Arrays.stream(actual).boxed().toList());
	}

	/**
	 * @param actual array object
	 * @return that iterable
	 */
	public static That.AssertedIterable<Double> that(double[] actual) {
		class Tested extends That.What<Iterable<Double>, That.AssertedIterable<Double>>
				implements That.AssertedIterable<Double> {}
		return new Tested().set(Arrays.stream(actual).boxed().toList());
	}

	/**
	 * @param actual long value or null
	 * @return that double
	 */
	public static That.AssertedString that(@Null String actual) {
		class Tested extends That.What<String, That.AssertedString> implements That.AssertedString {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual long value or null
	 * @return that double
	 */
	public static That.AssertedBoolean that(@Null Boolean actual) {
		class Tested extends That.What<Boolean, That.AssertedBoolean> implements That.AssertedBoolean {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual float value or null
	 * @return that double
	 */
	public static That.AssertedDouble that(@Null Float actual) {
		return that(actual == null ? null : actual.doubleValue());
	}

	/**
	 * @param actual long value or null
	 * @return that double
	 */
	public static That.AssertedDouble that(@Null Double actual) {
		class Tested extends That.What<Double, That.AssertedDouble> implements That.AssertedDouble {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual long value or null
	 * @return that long
	 */
	public static That.AssertedLong that(@Null Long actual) {
		class Tested extends That.What<Long, That.AssertedLong> implements That.AssertedLong {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual int value or null
	 * @return that integer
	 */
	public static That.AssertedInt that(@Null Integer actual) {
		class Tested extends That.What<Integer, That.AssertedInt> implements That.AssertedInt {}
		return new Tested().set(actual);
	}

	/**
	 * @param actual short value or null
	 * @return that integer
	 */
	public static That.AssertedInt that(@Null Short actual) {
		return that(actual == null ? null : actual.intValue());
	}

	/**
	 * @param actual character value or null
	 * @return that integer
	 */
	public static That.AssertedInt that(@Null Character actual) {
		return that(actual == null ? null : (int) actual);
	}
}
