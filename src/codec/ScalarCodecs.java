package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

final class ScalarCodecs implements Codec.Factory<In, Out> {
	private static final Map<Class<?>, Codec<?, In, Out>> codecs = new HashMap<>();
	static {
		var intCodec = new IntegerCodec();
		var longCodec = new LongCodec();
		var doubleCodec = new DoubleCodec();
		var floatCodec = new FloatCodec();
		var booleanCodec = new BooleanCodec();

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

		codecs.put(String.class, new StringCodec());
	}
	private static final Class<?>[] classes = codecs.keySet().toArray(new Class<?>[0]);

	public Class<?>[] classes() {
		return classes.clone();
	}

	public @Null Codec<?, In, Out> tryCreate(
		Type type, Class<?> raw,
		Medium<? extends In, ? extends Out> medium,
		Codec.Lookup<In, Out> lookup) {
		return codecs.get(raw);
	}

	private static class StringCodec extends Codec<String, In, Out> {
		public void encode(Out out, String s) throws IOException {
			out.putString(s);
		}

		public @Null String decode(In in) throws IOException {
			return in.takeString();
		}
	}

	private static class IntegerCodec extends Codec<Integer, In, Out> {
		public void encode(Out out, Integer i) throws IOException {
			out.putInt(i);
		}

		public @Null Integer decode(In in) throws IOException {
			return in.takeInt();
		}
	}

	private static class LongCodec extends Codec<Long, In, Out> {
		public void encode(Out out, Long i) throws IOException {
			out.putLong(i);
		}

		public @Null Long decode(In in) throws IOException {
			return in.takeLong();
		}
	}

	private static class DoubleCodec extends Codec<Double, In, Out> {
		public void encode(Out out, Double d) throws IOException {
			out.putDouble(d);
		}

		public @Null Double decode(In in) throws IOException {
			return in.takeDouble();
		}
	}

	private static class FloatCodec extends Codec<Float, In, Out> {
		public void encode(Out out, Float d) throws IOException {
			out.putDouble((double) d);
		}

		public @Null Float decode(In in) throws IOException {
			return (float) in.takeDouble();
		}
	}

	private static class BooleanCodec extends Codec<Boolean, In, Out> {
		public void encode(Out out, Boolean b) throws IOException {
			out.putBoolean(b);
		}

		public @Null Boolean decode(In in) throws IOException {
			return in.takeBoolean();
		}
	}
}
