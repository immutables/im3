package io.immutables.that;

import io.immutables.meta.Null;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Wrapper around test subject providing assertion check methods.
 * @param <T> object under test type
 * @param <S> matcher type
 * @see Assert
 * @see Condition
 * @see Object
 * @see Runnable
 * @see Iterable
 * @see Optional
 * @see String
 * @see Int
 * @see Long
 * @see Double
 * @see Boolean
 */
public interface That<T, S extends That<T, S>> {
	/**
	 * This is not a matcher method. Always throws {@link UnsupportedOperationException}.
	 * @deprecated Use {@link Object#equalTo(java.lang.Object)} instead.
	 */
	@Deprecated
	@Override
	boolean equals(java.lang.Object obj);

	/**
	 * This is not a matcher method. Always throws {@link UnsupportedOperationException}.
	 * @deprecated don't use this method.
	 */
	@Deprecated
	@Override
	int hashCode();

	/**
	 * Turns any matcher into a plain object matcher. If some matcher (like {@link Iterable}) streamlines matchers for
	 * iterable types, if you wish to resort to object matchers, you can always call {@link #just()} to re-wrap actual
	 * value into just plain object matcher. Primitive types are considered auto-wrapped in corresponding boxed types.
	 */
	default Object<T> just() {
		return Assert.that(What.getNullable(this));
	}

	/** Tests for certain conditions without (no wrapped value). */
	interface Condition extends That<Void, Condition> {
		/**
		 * Fails if condition is {@code false}.
		 * @param condition to check
		 */
		default void is(boolean condition) {
			if (!condition) throw What.newAssertionError("expected true condition", "actual false");
		}

		/**
		 * Fails if condition is {@code true}
		 * @param condition to check
		 */
		default void not(boolean condition) {
			if (condition) throw What.newAssertionError("expected false condition", "actual true");
		}

		/**
		 * Fails if invoked. Should be used for unreachable during successful pass test code.
		 *
		 * <pre>
		 * try {
		 *  somethingThatThrows();
		 *  that().unreachable();
		 * } catch (Exception ex) {
		 * </pre>
		 * @see Assert#that(java.lang.Runnable)
		 */
		default void unreachable() {
			throw What.newAssertionError("expected unreachable", "...and yet we are here");
		}
	}

	/** That runnable or lambda/block. */
	interface Runnable extends That<java.lang.Runnable, Runnable> {
		/**
		 * Fails if that runnable doesn't throw a Throwable of the expected type
		 * (or a subtype) when called.
		 * @param thrownType type of expected throwable
		 * @return that exception checker
		 */
		default <E extends Throwable> Object<E> thrown(Class<E> thrownType) {
			try {
				java.lang.Runnable runnable = What.get(this);
				runnable.run();
			} catch (Throwable throwable) {
				if (thrownType.isInstance(throwable)) {
					return Assert.that(thrownType.cast(throwable));
				}
				throw What.newAssertionError(
						"expected thrown " + thrownType.getCanonicalName(),
						"actual: " + throwable.getClass().getCanonicalName() + ": " + throwable.getMessage());
			}
			throw What.newAssertionError(
				"expected thrown " + thrownType.getCanonicalName(),
				"actual thrown nothing");
		}
	}

	/**
	 * Checks that string.
	 */
	interface String extends That<java.lang.String, String> {

		/**
		 * Fails if that string doesn't equal expected value
		 * @param expected value
		 */
		default void is(java.lang.String expected) {
			java.lang.String actual = What.get(this);
			if (!expected.equals(actual)) {
				List<java.lang.String> diff = Diff.diff(expected, actual);
				throw What.newAssertionError("expected: " + diff.get(0), "actual: " + diff.get(1));
			}
		}

		/** Fails if that string is not empty */
		default void isEmpty() {
			java.lang.String actual = What.get(this);
			if (!actual.isEmpty()) {
				throw What.newAssertionError("expected empty string", "actual: " + Diff.trim(actual));
			}
		}

		/** Fails if that string is empty */
		default void notEmpty() {
			java.lang.String actual = What.get(this);
			if (actual.isEmpty()) {
				throw What.newAssertionError("expected nonempty string", "actual empty");
			}
		}

		/**
		 * Fails if that string has length other thans
		 * @param expectedLength expected length
		 */
		default void hasLength(int expectedLength) {
			java.lang.String actual = What.get(this);
			if (actual.length() != expectedLength) {
				throw What.newAssertionError(
						"expected length: " + expectedLength,
						"actual length: " + actual.length() + " — " + Diff.trim(What.get(this)));
			}
		}

		/**
		 * Fails if that string doesn't contain substring.
		 * @param substring expected substring
		 */
		default void contains(java.lang.String substring) {
			java.lang.String actual = What.get(this);
			if (!actual.contains(substring)) {
				throw What.newAssertionError(
						"expected string containing: " + Diff.trim(substring),
						"actual: " + Diff.trim(actual));
			}
		}

		/**
		 * Fails if that string doesn't start with prefix
		 * @param prefix expected prefix
		 */
		default void startsWith(java.lang.String prefix) {
			java.lang.String actual = What.get(this);
			if (!actual.startsWith(prefix)) {
				throw What.newAssertionError(
						"expected string starts with: " + Diff.trim(prefix),
						"actual: " + Diff.diff(actual, prefix).get(0));
			}
		}

		/**
		 * Fails if that string doesn't start with suffix
		 * @param suffix expected suffix
		 */
		default void endsWith(java.lang.String suffix) {
			java.lang.String actual = What.get(this);
			if (!actual.endsWith(suffix)) {
				throw What.newAssertionError(
						"expected string starts with: " + Diff.trim(suffix),
						"actual: " + Diff.diff(actual, suffix).get(0));
			}
		}

		/**
		 * Fails if that string doesn't matches regex pattern
		 * @param regex expected pattern
		 */
		default void matches(java.lang.String regex) {
			java.lang.String actual = What.get(this);
			try {
				if (!actual.matches(regex)) {
					throw What.newAssertionError(
							"expected string to match /" + regex + "/",
							"actual: " + actual);
				}
			} catch (PatternSyntaxException ex) {
				throw What.newAssertionError(
						"expected string to match /" + regex + "/",
						"pattern syntax error: " + ex.getMessage());
			}
		}
	}

	/**
	 * Checks that boolean
	 */
	interface Boolean extends That<java.lang.Boolean, Boolean> {
		/**
		 * Fails if that is not of expected boolean value or if null.
		 * @param trueOfFalse expected value
		 */
		default void is(boolean trueOfFalse) {
			java.lang.Boolean b = What.get(this);
			if (trueOfFalse != b) {
				throw What.newAssertionError("expected: " + trueOfFalse, "actual: " + b);
			}
		}

		/**
		 * Fails if that is {@code false} with specified message.
		 * @param message assertion error message
		 */
		default void orFail(java.lang.String message) {
			if (!What.get(this)) {
				throw What.newAssertionError(message);
			}
		}
	}

	/** Checks that object */
	interface Object<T> extends That<T, Object<T>> {

		/**
		 * @return always {@code this}
		 * @deprecated Already regular object matcher.
		 */
		@Override
		@Deprecated
		default Object<T> just() {
			return this;
		}

		/** Fails if that object referece is not {@code null} */
		default void isNull() {
			@Null T actual = What.getNullable(this);
			if (actual != null) {
				throw What.newAssertionError("expected: null", "actual: " + actual);
			}
		}

		/** Fails if that object reference is {@code null} */
		default Object<T> notNull() {
			What.get(this);
			return this;
		}

		/**
		 * Fails if object is not of expected type (or subtype).
		 * @param <C> expected type
		 * @param type expected class object
		 * @return check that downcasted object
		 */
		default <C extends T> Object<C> instanceOf(Class<C> type) {
			T actualRef = What.get(this);
			if (!type.isInstance(actualRef)) {
				throw What.newAssertionError(
						"expected instance of " + type.getCanonicalName(),
						"actual: " + What.showReference(actualRef) + What.showToStringDetail(actualRef));
			}
			@SuppressWarnings("unchecked") // safe cast after runtime check
			Object<C> that = (Object<C>) this;
			return that;
		}

		/**
		 * Fails if that object is not predicated by expression
		 * @param predicate predicate expression
		 * @return check that object
		 */
		default Object<T> is(Predicate<T> predicate) {
			@Null T actual = What.getNullable(this);
			if (!predicate.test(actual)) {
				throw What.newAssertionError(
						"expected matching predicate " + What.showNonDefault(predicate),
						"actual: " + actual);
			}
			return this;
		}

		/**
		 * Fails if that object reference doesn't refers to the exact same expected reference
		 * @param expected object reference (allows null)
		 */
		default void same(@Null T expected) {
			@Null T actual = What.getNullable(this);
			if (actual != expected) {
				throw What.newAssertionError(
						"expected same: "
								+ What.showReference(expected)
								+ What.showToStringDetail(expected),
						"actual: " + What.showReference(actual) + What.showToStringDetail(actual));
			}
		}

		/**
		 * Fails if that object reference refers to the exact same expected reference.
		 * @param expected not same reference (allows null)
		 */
		default void notSame(@Null T expected) {
			@Null T actual = What.getNullable(this);
			if (actual == expected) {
				throw What.newAssertionError(
						"expected not same: " + What.showReference(expected) + What.showToStringDetail(expected),
						"actually was the same reference");
			}
		}

		/**
		 * Fails if that object's result of calling {@code toString()} doesn't equal to expected string
		 * @param expectedToString expected {@code toString} value
		 */
		default void hasToString(java.lang.String expectedToString) {
			Objects.requireNonNull(expectedToString);

			java.lang.String actual = What.get(this).toString();
			if (!actual.equals(expectedToString)) {
				List<java.lang.String> diff = Diff.diff(expectedToString, actual);
				throw What.newAssertionError("expected: " + diff.get(0), "actual: " + diff.get(1));
			}
		}

		/**
		 * Fails if that object is not equivalent (via {@link java.lang.Object#equals(java.lang.Object)})
		 * to the expected object.
		 * @param expected equivalent object
		 */
		default void equalTo(T expected) {
			Objects.requireNonNull(expected);

			T actual = What.get(this);
			if (!actual.equals(expected)) {
				java.lang.String as = actual.toString();
				java.lang.String es = expected.toString();

				if (as.equals(es)) {
					throw What.newAssertionError(
							"expected: " + What.showReference(expected) + What.showToStringDetail(expected),
							"actual: " + What.showReference(actual) + What.showToStringDetail(actual));
				}

				List<java.lang.String> diff = Diff.diff(es, as);
				throw What.newAssertionError("expected: " + diff.get(0), "actual: " + diff.get(1));
			}
		}

		/**
		 * Fails if that object is equivalent (via {@link java.lang.Object#equals(java.lang.Object)})
		 * to the expected object.
		 * @param expectedNonEquivalent expected not equivalent object
		 */
		default void notEqual(T expectedNonEquivalent) {
			Objects.requireNonNull(expectedNonEquivalent);

			T actual = What.get(this);
			if (actual.equals(expectedNonEquivalent)) {
				if (actual == expectedNonEquivalent) {
					throw What.newAssertionError(
							"expected not equal: "
									+ What.showReference(expectedNonEquivalent)
									+ What.showToStringDetail(expectedNonEquivalent),
							"actual was the same object");
				}

				java.lang.String as = actual.toString();
				java.lang.String es = expectedNonEquivalent.toString();

				if (as.equals(es)) {
					throw What.newAssertionError(
							What.showReference(expectedNonEquivalent) + What.showToStringDetail(expectedNonEquivalent),
							What.showReference(actual) + What.showToStringDetail(actual));
				}

				throw What.newAssertionError(
						What.showReference(expectedNonEquivalent) + What.showToStringDetail(expectedNonEquivalent),
						What.showReference(actual) + What.showToStringDetail(actual));
			}
		}
	}

	interface Optional<T> extends That<java.util.Optional<T>, Optional<T>> {

		default void isEmpty() {
			java.util.Optional<T> actual = What.get(this);
			if (actual.isPresent()) {
				throw What.newAssertionError(
						"expected: Optional.empty()",
						"actual: Optional.of(" + actual.get() + ")");
			}
		}

		default void isPresent() {
			java.util.Optional<T> actual = What.get(this);
			if (actual.isEmpty()) {
				throw What.newAssertionError(
						"expected: expected present Optional",
						"actual: Optional.empty()");
			}
		}

		default void isOf(T expected) {
			java.util.Optional<T> actualOptional = What.get(this);
			java.util.Optional<T> expectedOptional = java.util.Optional.of(expected);
			if (!actualOptional.equals(expectedOptional)) {
				throw What.newAssertionError(
						"expected: Optional.of(" + expected + ")",
						"actual: " + actualOptional.map(t -> "Optional.of(" + t + ")")
								.orElse("Optional.empty()"));
			}
		}
	}

	/** Checks that iterable (collection, collection view, array or any iterable) */
	interface Iterable<T> extends That<java.lang.Iterable<T>, Iterable<T>> {
		default void notEmpty() {
			List<T> list = What.getList(this);
			if (list.isEmpty()) {
				throw What.newAssertionError("expected non empty", "actual: " + Diff.trim(What.get(this)));
			}
		}

		default void isEmpty() {
			List<T> list = What.getList(this);
			if (!list.isEmpty()) {
				throw What.newAssertionError("expected empty", "actual: " + Diff.trim(What.get(this)));
			}
		}

		default void hasSize(int expectedSize) {
			List<T> list = What.getList(this);
			if (list.size() != expectedSize) {
				throw What.newAssertionError(
						"expected size: " + expectedSize,
						"actual size: " + list.size() + " — " + Diff.trim(What.get(this)));
			}
		}

		default void has(T expectedElement) {
			List<T> list = What.getList(this);
			if (!list.contains(expectedElement)) {
				throw What.newAssertionError(
						"expected element: " + expectedElement,
						"actual none — " + Diff.trim(What.get(this)));
			}
		}

		default void isOf(java.lang.Iterable<T> expectedElements) {
			List<T> actualList = What.getList(this);
			List<T> expectedList = new ArrayList<>();
			expectedElements.forEach(expectedList::add);

			if (!actualList.equals(expectedList)) {
				List<java.lang.String> diff = Diff.diff(
						What.showElements(expectedList),
						What.showElements(actualList));

				throw What.newAssertionError(
						"expected elements: " + diff.get(0),
						"actual elements: " + diff.get(1));
			}
		}

		@SuppressWarnings("unchecked")
		default void isOf(T... expectedElements) {
			isOf(Arrays.asList(expectedElements));
		}

		@SuppressWarnings("unchecked")
		default void hasAll(T... expectedElements) {
			hasAll(Arrays.asList(expectedElements));
		}

		@SuppressWarnings("unchecked")
		default void hasOnly(T... expectedElements) {
			hasOnly(Arrays.asList(expectedElements));
		}

		default void hasAll(java.lang.Iterable<T> expectedElements) {
			List<T> actualElements = What.getList(this);
			List<T> missingElements = new ArrayList<>();
			for (T e : expectedElements) {
				if (!actualElements.contains(e)) {
					missingElements.add(e);
				}
			}
			if (!missingElements.isEmpty()) {
				throw What.newAssertionError(
						"expected has all: " + What.showElements(expectedElements),
						"actual: missing " + What.showElements(missingElements) + " — " + Diff.trim(actualElements));
			}
		}

		default void hasOnly(java.lang.Iterable<T> elements) {
			List<T> remainingElements = What.getList(this);
			List<T> expectedElements = new ArrayList<>();
			List<T> missingElements = new ArrayList<>();

			for (T e : elements) {
				expectedElements.add(e);
				if (!remainingElements.contains(e)) {
					missingElements.add(e);
				} else {
					remainingElements.remove(e);
				}
			}

			if (!missingElements.isEmpty() || !remainingElements.isEmpty()) {
				java.lang.String actual = "";
				if (!missingElements.isEmpty()) {
					actual += "missing " + What.showElements(missingElements) + "; ";
				}
				if (!remainingElements.isEmpty()) {
					actual += "extra  " + What.showElements(remainingElements);
				}
				throw What.newAssertionError(
						"expected only: " + What.showElements(expectedElements),
						"actual: " + actual);
			}
		}
	}

	interface Double extends That<java.lang.Double, Double> {

		default void bitwiseIs(double expected) {
			double actual = What.get(this);
			if (java.lang.Double.doubleToRawLongBits(actual) != java.lang.Double.doubleToRawLongBits(expected)) {
				throw What.newAssertionError("expected: " + expected, "actual: " + actual);
			}
		}

		default void is(DoublePredicate predicate) {
			double actual = What.get(this);
			if (!predicate.test(actual)) {
				throw What.newAssertionError(
						"expected matching predicate " + What.showNonDefault(predicate),
						"actual: " + actual);
			}
		}

		default void withinOf(double epsilon, double expected) {
			double actual = What.get(this);
			if (Math.abs(actual - expected) > epsilon) {
				throw What.newAssertionError(
						"expected within ±" + epsilon + " of " + expected,
						"actual: " + actual);
			}
		}

		default void isNaN() {
			double actual = What.get(this);
			if (!java.lang.Double.isNaN(actual)) {
				throw What.newAssertionError("expected NaN", "actual: " + actual);
			}
		}

		default void isFinite() {
			double actual = What.get(this);
			if (!java.lang.Double.isFinite(actual)) {
				throw What.newAssertionError("expected finite double", "actual: " + actual);
			}
		}

		default void isInfinite() {
			double actual = What.get(this);
			if (!java.lang.Double.isInfinite(actual)) {
				throw What.newAssertionError("expected infinite double", "actual: " + actual);
			}
		}
	}

	interface Int extends That<Integer, Int> {
		default void is(IntPredicate predicate) {
			int actual = What.get(this);
			if (!predicate.test(actual)) {
				throw What.newAssertionError(
						"expected matching predicate " + What.showNonDefault(predicate),
						"actual: " + actual);
			}
		}

		default void is(int expected) {
			int actual = What.get(this);
			if (actual != expected) {
				throw What.newAssertionError("expected: " + expected, "actual: " + actual);
			}
		}
	}

	interface Long extends That<java.lang.Long, Long> {
		default void is(LongPredicate predicate) {
			long actual = What.get(this);
			if (!predicate.test(actual)) {
				throw What.newAssertionError(
						"expected matching predicate " + What.showNonDefault(predicate),
						"actual: " + actual);
			}
		}

		default void is(long expected) {
			long actual = What.get(this);
			if (actual != expected) {
				throw What.newAssertionError("expected: " + expected, "actual: " + actual);
			}
		}
	}

	/**
	 * This support class is mandatory to extend and decorate with any {@link That} interfaces (providing default
	 * methods). Implementing classes are usually private or local, but {@link That}-interfaces are public.
	 * @param <T> type of value to check
	 * @param <S> self-type of checker.
	 */
	abstract class What<T, S extends That<T, S>> implements That<T, S> {
		private @Null T value;
		private boolean isSet;

		/**
		 * Set value under test
		 * @param value to be tested
		 * @return this for chained invokation
		 */
		@SuppressWarnings("unchecked")
		public final S set(@Null T value) {
			this.value = value;
			this.isSet = true;
			return (S) this;
		}

		private @Null T get() {
			if (!isSet) throw new IllegalStateException("What.set(value) must be called on wrapper");
			return value;
		}

		@Deprecated
		@Override
		public final boolean equals(java.lang.Object obj) {
			throw new UnsupportedOperationException();
		}

		@Deprecated
		@Override
		public final int hashCode() {
			throw new UnsupportedOperationException();
		}

		@Override
		public final java.lang.String toString() {
			return "that(" + value + ")";
		}

		/**
		 * Extracts actual value from wrapper. Trips on {@code null} immediately: raises assertion error. It is mandatory
		 * that input argument extends {@link What}.
		 * @param <T> type of value
		 * @param <S> type of checker.
		 * @param that wrapper user to extract actual value
		 * @return unwrapped actual value
		 * @see #getNullable(That) to allow nullable values
		 */
		static <T, S extends That<T, S>> T get(That<T, S> that) {
			@Null T value = ((What<T, S>) that).get();
			if (value != null) return value;
			throw newAssertionError("non-null expected");
		}

		/**
		 * The same as {@link #get(That that)}, but allows actual value to be {@code null}
		 * @param <T> type of value
		 * @param <S> type of checker which extends {@link That}.
		 * @param that wrapper user to extract actual value
		 * @return unwrapped actual value
		 */
		static @Null <T, S extends That<T, S>> T getNullable(That<T, S> that) {
			return ((What<T, S>) that).get();
		}

		/**
		 * Factory for well-suited assertion error. Typical usage is to format via 2 strings, one for actuall value. While
		 * it is not mandatory to use this factory method to create assertion errors, if you choose to use it, this will
		 * provide very pretty stack trace filtering and source code extraction (if test sources are available as resources
		 * on the classpath).
		 * @param linesDescribingMismatch message lines
		 * @return properly constructed instance of {@link AssertionError}
		 */
		static AssertionError newAssertionError(java.lang.String... linesDescribingMismatch) {
			return new AssertionError(linesDescribingMismatch);
		}

		private static <T, S extends That<java.lang.Iterable<T>, S>> List<T> getList(
			That<java.lang.Iterable<T>, S> that) {
			java.lang.Iterable<T> actual = get(that);
			List<T> list = new ArrayList<>();
			actual.forEach(list::add);
			return list;
		}

		private static java.lang.String showElements(java.lang.Iterable<?> iterable) {
			return StreamSupport.stream(iterable.spliterator(), false)
					.map(Objects::toString)
					.collect(Collectors.joining(", "));
		}

		private static java.lang.String showNonDefault(java.lang.Object ref) {
			java.lang.String string = ref.toString();
			if (string.endsWith(identityHashCodeSuffix(ref))) return "";
			return string;
		}

		private static java.lang.String showReference(@Null java.lang.Object ref) {
			return ref == null ? "null" : (ref.getClass().getSimpleName() + identityHashCodeSuffix(ref));
		}

		private static java.lang.String identityHashCodeSuffix(java.lang.Object ref) {
			return "@" + Integer.toHexString(System.identityHashCode(ref));
		}

		private static java.lang.String showToStringDetail(@Null java.lang.Object ref) {
			if (ref == null) return "";
			return " — " + (ref instanceof java.lang.String ? ("\"" + ref + "\"") : ref);
		}
	}
}
