package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;

public class Codecs {
	private Codecs() {}

	public static <T, I extends In, O extends Out> Codec<T, I, O> nullSafe(Codec<T, I, O> original) {
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

	public final static class CaptureStringOut extends UnimplementedOut {
		public @Null String string;
		public NameIndex index(String... known) {
			return NameIndex.known(known);
		}

		public void putString(char[] chars, int offset, int length) throws IOException {
			string = String.valueOf(chars, offset, length);
		}

		public void putString(String s) throws IOException {
			string = s;
		}

		public void putString(NameIndex names, int index) throws IOException {
			string = names.name(index);
		}
	}

	public final static class RetrieveStringIn extends UnimplementedIn {
		private @Null String string;
		private boolean consumed;

		public void reset(String string) {
			this.string = string;
			this.consumed = false;
		}

		public NameIndex index(String... known) {
			return NameIndex.known(known);
		}

		public At peek() {
			return string != null && !consumed ? At.String : At.End;
		}

		public String takeString() throws IOException {
			//TODO TODO TODO
			/*if (string)
			return super.takeString();*/
			return null;
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
}
