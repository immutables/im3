package io.immutables.codec.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import io.immutables.codec.*;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;

public final class EmbeddedJson {
	private EmbeddedJson() {}

	public static Codec.Factory<In, Out> using(JsonFactory factory) {
		return (type, raw, medium, lookup) -> {
			if (raw == Jsons.class) {
				var argument = Types.getFirstArgument(type);
				// switch medium for embedded Json
				var codec = lookup.resolve(argument, Medium.Json);
				if (codec.isPresent()) {
					return new JsonsCodec<Object>(argument, codec.get(), factory);
				}
				return null;
			}
			return null;
		};
	}

	private static final class JsonsCodec<T> extends Codec<Jsons<T>, In, Out> implements Expecting {
		private final Type type;
		private final Codec<T, In, Out> codec;
		private final JsonFactory factory;

		JsonsCodec(Type type, Codec<T, In, Out> codec, JsonFactory factory) {
			this.type = type;
			this.codec = codec;
			this.factory = factory;
		}

		public void encode(Out out, Jsons<T> instance) throws IOException {
			var writer = new StringWriter();
			try (var g = factory.createGenerator(writer)) {
				codec.encode(new JsonGeneratorOut(g), instance.content());
			}
			out.putString(writer.toString());
		}

		public Jsons<T> decode(In in) throws IOException {
			try (var p = factory.createParser(in.takeString())) {
				return new Jsons<>(codec.decode(new JsonParserIn(p)));
			}
		}

		public boolean expects(In.At first) {
			return first == In.At.String;
		}

		public String toString() {
			return getClass().getSimpleName() + "<" + type + ">";
		}
	}
}
