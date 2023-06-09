package io.immutables.codec;

import io.immutables.meta.Null;
import java.io.IOException;
import java.util.function.Function;

public abstract class FromToStringCodec<T> extends DefaultingCodec<T, In, Out> implements Expecting {
	public abstract String toString(T instance);
	public abstract T fromString(String string);

	public Class<?>[] classes() {return new Class<?>[0];}

	public void encode(Out out, T instance) throws IOException {
		out.putString(toString(instance));
	}

	public @Null T decode(In in) throws IOException {
		return fromString(in.takeString());
	}

	public boolean expects(In.At first) {
		return first == In.At.String;
	}

	public static <T> FromToStringCodec<T> from(
		Function<T, String> toString, Function<String, T> fromString, Class<? extends T> type) {
		return new FromToStringCodec<>() {
			public String toString(T instance) {
				return toString.apply(instance);
			}

			public T fromString(String string) {
				return fromString.apply(string);
			}

			public Class<?>[] classes() {
				return new Class<?>[]{type};
			}

			public String toString() {
				return FromToStringCodec.class.getSimpleName() + "<" + type.getTypeName() + ">";
			}
		};
	}
}
