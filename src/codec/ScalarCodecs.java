package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class ScalarCodecs {
	private static final Map<Class<?>, Codec<?, In, Out>> codecs = new HashMap<>();
	static {
		var intCodec = new IntegerCodec();
		var longCodec = new LongCodec();
		var doubleCodec = new DoubleCodec();
		var floatCodec = new FloatCodec();
		var booleanCodec = new BooleanCodec();
		var voidCodec = new VoidCodec();

		codecs.put(Integer.class, intCodec);
		codecs.put(int.class, intCodec);
		codecs.put(Long.class, longCodec);
		codecs.put(long.class, longCodec);
		codecs.put(Double.class, doubleCodec);
		codecs.put(double.class, doubleCodec);
		codecs.put(Float.class, floatCodec);
		codecs.put(float.class, floatCodec);
		codecs.put(Boolean.class, booleanCodec);
		codecs.put(boolean.class, booleanCodec);
		codecs.put(Void.class, voidCodec);
		codecs.put(void.class, voidCodec);
		codecs.put(String.class, new StringCodec());
	}

	private static final Class<?>[] classes = codecs.keySet().toArray(new Class<?>[0]);

	public static Class<?>[] classes() {
		return classes.clone();
	}

	static final Codec.Factory<In, Out> Factory = (type, raw, medium, lookup) -> codecs.get(raw);

	private static class StringCodec extends Codec<String, In, Out> implements Expecting {
		public void encode(Out out, String s) throws IOException {
			out.putString(s);
		}

		public String decode(In in) throws IOException {
			return in.takeString();
		}

		public boolean expects(Token first) {
			return first == Token.String;
		}
	}

	private static class IntegerCodec extends Codec<Integer, In, Out> implements Expecting {
		public void encode(Out out, Integer i) throws IOException {
			out.putInt(i);
		}

		public Integer decode(In in) throws IOException {
			return in.takeInt();
		}

		public boolean expects(Token first) {
			return first == Token.Int;
		}
	}

	private static class LongCodec extends Codec<Long, In, Out> implements Expecting {
		public void encode(Out out, Long i) throws IOException {
			out.putLong(i);
		}

		public Long decode(In in) throws IOException {
			return in.takeLong();
		}

		public boolean expects(Token first) {
			return switch (first) {
				case Int, Long -> true;
				default -> false;
			};
		}
	}

	private static class DoubleCodec extends Codec<Double, In, Out> implements Expecting {
		public void encode(Out out, Double d) throws IOException {
			out.putDouble(d);
		}

		public Double decode(In in) throws IOException {
			return in.takeDouble();
		}

		public boolean expects(Token first) {
			return switch (first) {
				case Int, Long, Float -> true;
				default -> false;
			};
		}
	}

	private static class FloatCodec extends Codec<Float, In, Out> implements Expecting {
		public void encode(Out out, Float d) throws IOException {
			out.putDouble((double) d);
		}

		public Float decode(In in) throws IOException {
			return (float) in.takeDouble();
		}

		public boolean expects(Token first) {
			return switch (first) {
				case Int, Long, Float -> true;
				default -> false;
			};
		}
	}

	private static class BooleanCodec extends Codec<Boolean, In, Out> implements Expecting {
		public void encode(Out out, Boolean b) throws IOException {
			out.putBoolean(b);
		}

		public Boolean decode(In in) throws IOException {
			return in.takeBoolean();
		}

		public boolean expects(Token first) {
			return first == Token.True || first == Token.False;
		}
	}

	private static class VoidCodec extends DefaultingCodec<Void, In, Out> implements Expecting {
		public void encode(Out out, @Null Void v) throws IOException {
			out.putNull();
		}

		public @Null Void decode(In in) throws IOException {
			in.takeNull();
			return null;
		}

		public boolean hasDefault() {
			return true;
		}

		public boolean expects(Token first) {
			return first == Token.Null;
		}
	}
}
