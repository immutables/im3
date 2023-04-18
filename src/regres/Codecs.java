package io.immutables.regres;

import io.immutables.codec.*;
import io.immutables.codec.jackson.JsonGeneratorOut;
import io.immutables.codec.jackson.JsonParserIn;
import io.immutables.meta.Null;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonWriteContext;

public final class Codecs {
	private Codecs() {}

	public static Codec.Factory<In, Out> jsonsFactory(JsonFactory factory) {
		return (type, raw, medium, lookup) -> {
			if (raw == Jsons.class) {
				var argument = Types.getFirstArgument(type);
				return new JsonsCodec<>(argument, lookup.get(argument), factory);
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
				return new Jsons<T>(codec.decode(new JsonParserIn(p)));
			}
		}

		public boolean canExpect(In.At first) {
			return first == In.At.String;
		}

		public String toString() {
			return getClass().getSimpleName() + "<" + type + ">";
		}
	}
}
