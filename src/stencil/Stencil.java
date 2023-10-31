package io.immutables.stencil;

import io.immutables.meta.Null;
import io.immutables.meta.SkippableReturn;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Subclass stencil if you intend to use pure composition with other stencils,
 * and don't have to do any output technology specific context actions ({@see Privileged}).
 * If the stencil have to output raw output itself (i.e. not using other stencils), extend
 * {@link Stencil.Raw}
 */
public abstract class Stencil {
	final Current current = Current.current();

	/**
	 * Privileged base class provides access to "current" instance.
	 * and to reset output.
	 */
	public abstract static class Privileged extends Stencil {
		@SkippableReturn
		protected final @Null Output reset(@Null Output out) {
			@Null var o = current.out;
			current.out = out;
			return o;
		}

		protected final Current current() {
			return current;
		}
	}

	/**
	 * Raw base class provides access to the current output
	 */
	public abstract static class Raw extends Privileged {
		@Override public Output out() {
			return super.out();
		}

		@SkippableReturn
		public Output ln() {
			return out().ln();
		}

		@SkippableReturn
		public Output ifln() {
			return out().ifln();
		}

		@SkippableReturn
		public Output put(Object o) {
			return out().put(o);
		}

		@SkippableReturn
		public Output put(Object a0, Object a1) {
			return out().put(a0, a1);
		}

		@SkippableReturn
		public Output put(Object a0, Object a1, Object a2) {
			return out().put(a0, a1, a2);
		}

		@SkippableReturn
		public Output put(Object a0, Object a1, Object a2, Object a3) {
			return out().put(a0, a1, a2, a3);
		}

		@SkippableReturn
		public Output put(Object a0, Object a1, Object a2, Object a3, Object a4, Object... rest) {
			return out().put(a0, a1, a2, a3, a4, rest);
		}
	}

	Output out() {
		@Null var o = current.out;
		if (o != null) return o;
		throw new IllegalStateException("Current output is not set");
	}

	// package-private conversion of types to string in the current output context
	// cannot be a static method
	// Used by Template base class for generators, but we don't want
	// Template to extend Stencil.Raw or Stencil.Privileged
	String stringify(@Null Object value) {
		if (value instanceof String s) return s;
		if (value instanceof CharSequence s) return s.toString();
		if (value instanceof Supplier<?> s) {
			return toString(s.get());
		}
		if (value instanceof Runnable r) {
			@Null var outer = current.out;
			var inner = new Output();
			current.out = inner;
			r.run();
			current.out = outer;
			return inner.toString();
		}
		return toString(value);
	}

	private String toString(@Null Object result) {
		return result != null
			? result.toString()
			: Objects.toString(
			current.out != null
				? current.out.nullString
				: null);
	}
}
