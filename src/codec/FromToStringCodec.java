package io.immutables.codec;

import java.io.IOException;
import java.util.function.Function;

/**
 * Simple codec implementation used to easily construct custom codecs based on
 * to/from string conversion.
 * @see #from(Function, Function, Class) Standard factory for such codecs uses lambdas
 * @param <T> Type to convert to/from string
 */
public abstract class FromToStringCodec<T>
		extends DefaultingCodec<T, In, Out>
		implements Expecting {
	public abstract String toString(T instance);
	public abstract T fromString(String string);
	public abstract Class<?> rawClass();

	@Override public final void encode(Out out, T instance) throws IOException {
		out.putString(toString(instance));
	}

	@Override public final T decode(In in) throws IOException {
		return fromString(in.takeString());
	}

	@Override public final boolean expects(In.At first) {
		return first == In.At.String;
	}

	public static <T> FromToStringCodec<T> from(
			Function<T, String> toString,
			Function<String, T> fromString,
			Class<? extends T> type) {
		return new FromToStringCodec<>() {
			public String toString(T instance) {
				return toString.apply(instance);
			}

			public T fromString(String string) {
				return fromString.apply(string);
			}

			public Class<?> rawClass() {
				return type;
			}

			public String toString() {
				return FromToStringCodec.class.getSimpleName() + "<" + type.getTypeName() + ">";
			}
		};
	}
}
