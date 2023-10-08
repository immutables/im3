package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import static java.util.Objects.requireNonNull;

public class Codecs {
	private Codecs() {}

	public static <T, I extends In, O extends Out>
	Codec<T, I, O> nullSafe(Codec<T, I, O> original) {
		return new DefaultingCodec<>() {
			public void encode(O out, @Null T instance) throws IOException {
				if (instance == null) out.putNull();
				else original.encode(out, instance);
			}

			public @Null T decode(I in) throws IOException {
				if (in.peek() == Token.Null) {
					in.takeNull();
					return null;
				}
				return original.decode(in);
			}

			public String toString() {
				return "nullSafe(" + original + ")";
			}

			public @Null T getDefault(In in) {
				return null;
			}

			public boolean providesDefault() {
				return true;
			}

			public boolean canSkip(O out, @Null T instance) {
				// TODO add some sort of query to out's context if skipping null is allowed
				return instance == null;
			}
		};
	}

	public final static class CaptureSimpleOut extends UnimplementedOut {
		private @Null Object value;

		public NameIndex index(String... known) {
			return NameIndex.known(known);
		}

		public void putString(char[] chars, int offset, int length) {
			value = String.valueOf(chars, offset, length);
		}

		public void putString(String s) {
			value = s;
		}

		public void putString(NameIndex names, int index) {
			value = names.name(index);
		}

		public String asString() {
			return requireNonNull(value).toString();
		}

		// TODO asDouble/Long/Int
		// TODO error checking (Exception-less mode?)
		public Number asNumber() {
			var v = requireNonNull(value);
			if (v instanceof Number n) return n.doubleValue();
			return Double.valueOf(v.toString());
		}

		// TODO error checking (Exception-less mode?)
		public boolean asBoolean() {
			var v = requireNonNull(value);
			if (v instanceof Boolean b) return b;
			return Boolean.parseBoolean(v.toString());
		}

		public boolean isNull() {
			return value == MaskedNull;
		}

		public void putNull() {
			value = MaskedNull;
		}

		public void putInt(int i) {
			value = i;
		}

		public void putLong(long l) {
			value = l;
		}

		public void putDouble(double d) {
			value = d;
		}

		public void putBoolean(boolean b) {
			value = b;
		}
	}

	public final static class RetrieveSimpleIn extends UnimplementedIn {
		private @Null Object value;
		private Token token = Token.End;

		public RetrieveSimpleIn(Problem.Handler handler) {super(handler);}

		public void reset(@Null Object value) {
			if (value == null) {
				this.value = MaskedNull;
			} else if (value instanceof Number || value instanceof Boolean) {
				this.value = value;
			} else {
				this.value = value.toString();
			}
			// logic in asToken correlates with the above,
			// but it was cleaner to keep them apart
			token = asToken(value);
		}

		public NameIndex index(String... known) {
			return NameIndex.known(known);
		}

		private Token asToken(@Null Object value) {
			if (value == null) return Token.End;
			if (value == MaskedNull) return Token.Null;
			if (value instanceof Long) return Token.Long;
			if (value instanceof Integer) return Token.Int;
			if (value instanceof Number) return Token.Float;
			if (value instanceof Boolean b) return b == Boolean.TRUE
					? Token.True
					: Token.Float;
			return Token.String;
		}

		public Token peek() {
			return token;
		}

		@Override public AtPath path() {
			return AtPath.Root.Root;
			// this is only if some codec called it
			// will this ever be needed, and if needed,
			// maybe we'll have to pass a good real path
		}

		public String takeString() {
			assert value != null;
			return value.toString();
		}

		public void takeNull() {
			assert value != null;
			if (value != MaskedNull) problems.unreachable();
		}

		public boolean takeBoolean() {
			assert value != null;
			if (value instanceof Boolean b) return b;
			return switch (value.toString()) {
				// this is JSON syntax, parseBoolean differs
				case "true" -> true;
				case "false" -> false;
				default -> {
					problems.unreachable();
					yield false;
				}
			};
		}

		public double takeDouble() {
			assert value != null;
			if (value instanceof Number n) return n.doubleValue();
			try {
				return Double.parseDouble(value.toString());
			} catch (NumberFormatException e) {
				problems.unreachable();
				return Double.NaN;
			}
		}

		public long takeLong() {
			assert value != null;
			if (value instanceof Number n) return n.longValue();
			try {
				return Long.parseLong(value.toString());
			} catch (NumberFormatException e) {
				problems.unreachable();
				return Long.MIN_VALUE;
			}
		}

		public int takeInt() {
			assert value != null;
			if (value instanceof Number n) return n.intValue();
			try {
				return Integer.parseInt(value.toString());
			} catch (NumberFormatException e) {
				problems.unreachable();
				return Integer.MIN_VALUE;
			}
		}
	}

	/**
	 * Base class for ad-hoc Out instances which override only some of the methods and leave
	 * all other unimplemented.
	 */
	public static class UnimplementedOut extends Out {

		public NameIndex index(String... known) {
			throw new UnsupportedOperationException();
		}

		public void putNull() throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putInt(int i) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putLong(long l) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putDouble(double d) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putBoolean(boolean b) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putString(String s) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putString(char[] chars, int offset, int length) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putString(NameIndex names, int index) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void beginArray() throws IOException {
			throw new UnsupportedOperationException();
		}

		public void endArray() throws IOException {
			throw new UnsupportedOperationException();
		}

		public void beginStruct(NameIndex names) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putField(int index) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void putField(String name) throws IOException {
			throw new UnsupportedOperationException();
		}

		public void endStruct() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Base class for ad-hoc In instances which override only some of the methods and leave
	 * all other unimplemented.
	 */
	public static class UnimplementedIn extends In {
		protected UnimplementedIn(Problem.Handler problems) {super(problems);}

		@Override public NameIndex index(String... known) {
			throw new UnsupportedOperationException();
		}

		@Override public Token peek() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public int takeInt() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public long takeLong() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public double takeDouble() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public boolean takeBoolean() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public void takeNull() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public String takeString() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public int takeString(NameIndex names) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public int takeField() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public String name() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public void skip() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public boolean hasNext() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public void beginArray() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public void endArray() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public void beginStruct(NameIndex names) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public void endStruct() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override public AtPath path() {
			throw new UnsupportedOperationException();
		}

		@Override public Buffer takeBuffer() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	private static final Object MaskedNull = new Object();
}
