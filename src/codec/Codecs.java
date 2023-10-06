package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.util.NoSuchElementException;
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
				if (in.peek() == In.At.Null) return null;
				return original.decode(in);
			}

			public String toString() {
				return "nullSafe(" + original + ")";
			}

			public @Null T getDefault() {
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

		public Number asNumber() {
			@Null var v = value;
			if (v == null) throw new NullPointerException();
			if (v instanceof Number n) return n;
			return Double.valueOf(v.toString());
		}

		public boolean asBoolean() {
			@Null var v = value;
			if (v == null) throw new NullPointerException();
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

		public RetrieveSimpleIn reset(@Null Object value) {
			this.value = value == null ? MaskedNull : value;
			return this;
		}

		public NameIndex index(String... known) {
			return NameIndex.known(known);
		}

		public At peek() {
			return value != null ? At.String : At.End;
		}

		public String takeString(){
			return requireNonNull(value).toString();
		}

		public void takeNull() {
			if (value != MaskedNull) throw new NoSuchElementException("is not null");
		}

		public boolean takeBoolean() {
			@Null var v = value;
			if (v == null) throw new NullPointerException();
			if (v instanceof Boolean b) return b;
			return Boolean.parseBoolean(v.toString());
		}

		public double takeDouble() {
			@Null var v = value;
			if (v == null) throw new NullPointerException();
			if (v instanceof Number n) return n.doubleValue();
			return Double.parseDouble(v.toString());
		}

		public long takeLong() {
			@Null var v = value;
			if (v == null) throw new NullPointerException();
			if (v instanceof Number n) return n.longValue();
			return Long.parseLong(v.toString());
		}

		public int takeInt() {
			@Null var v = value;
			if (v == null) throw new NullPointerException();
			if (v instanceof Number n) return n.intValue();
			return Integer.parseInt(v.toString());
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

		public NameIndex index(String... known) {
			throw new UnsupportedOperationException();
		}

		public At peek() throws IOException {
			throw new UnsupportedOperationException();
		}

		public int takeInt() throws IOException {
			throw new UnsupportedOperationException();
		}

		public long takeLong() throws IOException {
			throw new UnsupportedOperationException();
		}

		public double takeDouble() throws IOException {
			throw new UnsupportedOperationException();
		}

		public boolean takeBoolean() throws IOException {
			throw new UnsupportedOperationException();
		}

		public void takeNull() throws IOException {
			throw new UnsupportedOperationException();
		}

		public String takeString() throws IOException {
			throw new UnsupportedOperationException();
		}

		public int takeString(NameIndex names) throws IOException {
			throw new UnsupportedOperationException();
		}

		public int takeField() throws IOException {
			throw new UnsupportedOperationException();
		}

		public String name() throws IOException {
			throw new UnsupportedOperationException();
		}

		public void skip() throws IOException {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() throws IOException {
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

		public void endStruct() throws IOException {
			throw new UnsupportedOperationException();
		}

		public String path() throws IOException  {
			throw new UnsupportedOperationException();
		}

		public Buffer takeBuffer() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	private static final Object MaskedNull = new Object();
}
