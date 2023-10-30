package io.immutables.codec;

import io.immutables.meta.Null;
import io.immutables.meta.NullUnknown;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deserialization problems arising from mismatch of the expected types schema and actual data.
 * These are not related to JSON or binary syntax or low-level encoding.
 * The goal is to either work in fail-fast mode throwing an exception
 * (see {@link #ThrowingHandler}) and terminate decoding, or have exception-less mode where
 * we carefully accumulate problems up to some limit, and then we can use those to report
 * problems back (to a log file or in response to client).
 */
public sealed interface Problem {
	AtPath at();

	record MissingField(AtPath at, String field, Type componentType, Type recordType)
			implements Problem {}

	record UnknownField(AtPath at, String field, Type recordType, Token token)
			implements Problem {}

	record CannotInstantiate(AtPath at, Type recordType, String message)
			implements Problem {}

	record UnexpectedToken(AtPath at, String expected, String actual, Token token)
			implements Problem {}

	record NoMatchingCase(AtPath at, Type sealedType)
			implements Problem {}

	/**
	 * Handler provides the strategy how to handle problems. Either turn them in fatal
	 * {@link IOException} like {@link #ThrowingHandler} does, or to operate in exception-less manner
	 * to collect problems (maybe up to some limit). This requires decoders to cooperate to properly
	 * handle and propagate the state of having problems, carefully clearing such a state
	 * ({@link #raised()}) when such problem is noted, proceeding safely to the next element/or
	 * skipping everything, and make sure to raise the flag again in the end (by
	 * {@link #enque(Problem)}).
	 */
	abstract class Handler {
		/**
		 * Enqueues problem, raising error flag. Implementation may choose to be fail-fast
		 * and to throw {@link IOException} instead of collecting problems.
		 * Handler may also drop problem after reaching some limit and raising
		 * {@link #isOverflowed()} flag.
		 */
		public abstract void enque(Problem problem) throws IOException;
		/** Lists collected problems */
		public abstract List<Problem> list();
		/** There were more problems than listed, which were dropped after reaching some limit */
		public abstract boolean isOverflowed();
		/**
		 * Returns {@code true} if any problems were enqueued and clears this flag.
		 * (The flag is cleared after this call!)
		 */
		public abstract boolean raised();
		/**
		 * Sneaky null is returned to express that no valid result available from the codec.
		 * Error flag is always raised in this method.
		 */
		public abstract <T> @NullUnknown T unreachable();
	}

	/**
	 * This problem handler prefers to not enque problems, but immediately turn them into thrown
	 * {@link IOException}. Other methods are stubbed with default/negative values, which are not
	 * important if exception will be thrown and will terminate the decoding. Codecs should not catch
	 * {@link IOException} in general, but only very specific ones, which are not related to IO or
	 * JSON/serialization format, and only in very special cases.
	 */
	Handler ThrowingHandler = new Handler() {
		@Override public void enque(Problem problem) throws IOException {
			throw new IOException(Problem.toString(problem));
		}

		@Override public List<Problem> list() {
			return List.of();
		}

		@Override public boolean isOverflowed() {
			return false;
		}

		@Override public boolean raised() {
			return false;
		}

		@Override public <T> @Null T unreachable() {
			return null;
		}

		@Override public String toString() {
			return Problem.class.getSimpleName() + ".ThrowingHandler";
		}
	};

	/* maybe future functionality, let just have simple limit
	record Limits(
			int total,
			int perArray,
			int perObject) {}*/

	static Handler collectingHandler(int limit) {
		return new Handler() {
			private final List<Problem> enqued = new ArrayList<>();
			private boolean raised;
			private boolean overflowed;

			@Override public void enque(Problem problem) throws IOException {
				if (enqued.size() < limit) {
					enqued.add(problem);
				} else {
					// drop any problem once we reached limit, resetting overflow flag
					overflowed = true;
				}
				// in any case, overflowed or not, we raise the frag
				raised = true;
			}

			@Override public List<Problem> list() {
				return Collections.unmodifiableList(enqued);
			}

			@Override public boolean isOverflowed() {
				return overflowed;
			}

			@Override public boolean raised() {
				boolean was = raised;
				if (was) raised = false;
				return was;
			}

			/**
			 * Used to return an "unreachable" value, a sneaky {@code null} which should
			 * not be touched. It also ensures that error flag is raised.
			 * Mostly it should be used in codecs as:
			 * <pre>
			 * if (problem) return in.problems.unreachable();
			 * </pre>
			 */
			@Override public <T> @NullUnknown T unreachable() {
				raised = true;
				// this would be a "sneaky" null, if cooperation implemented
				// properly in codecs, no-one should touch this value and for
				// the type/null checker it should appear as non-null
				return null;
			}

			@Override public String toString() {
				return "%s.collectingHandler(limit=%s)[%d problems]"
						.formatted(Problem.class.getSimpleName(), limit, enqued.size());
			}
		};
	}

	static String toString(Problem problem) {
		if (problem instanceof MissingField f) {
			return "at %s: Missing field '%s' of type %s within record type %s"
					.formatted(f.at, f.field,
							f.componentType.getTypeName(), f.recordType.getTypeName());
		}
		if (problem instanceof UnexpectedToken t) {
			return "at %s: Expected `%s`, but was `%s`, token: %s"
					.formatted(t.at, t.expected, t.actual, t.token);
		}
		if (problem instanceof CannotInstantiate c) {
			return "at %s: Cannot instantiate %s: %s"
					.formatted(c.at, c.recordType.getTypeName(), c.message);
		}
		if (problem instanceof NoMatchingCase m) {
			return "at %s: No matching case for variant type %s"
					.formatted(m.at, m.sealedType.getTypeName());
		}
		if (problem instanceof UnknownField u) {
			return "at %s: Unknown field '%s' for type %s, value token: %s"
					.formatted(u.at, u.field, u.recordType.getTypeName(), u.token);
		}
		// TODO switch to a switch when on Java 21+
		throw new AssertionError("Not an exhaustive match");
	}
}
