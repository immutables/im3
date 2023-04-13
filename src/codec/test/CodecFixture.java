package io.immutables.codec.test;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.Out;
import io.immutables.codec.jackson.JsonGeneratorOut;
import io.immutables.codec.jackson.JsonParserIn;
import io.immutables.meta.Null;
import io.immutables.that.Assert;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

abstract class CodecFixture {
	protected CodecFixture() {}

	protected static final JsonFactory jsonFactory = new JsonFactoryBuilder()
		.enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
		.disable(JsonWriteFeature.QUOTE_FIELD_NAMES)
		.build();

	protected static <T> String toJson(Codec<T, In, Out> codec, T instance) throws IOException {
		var w = new StringWriter();
		var g = jsonFactory.createGenerator(w);
		var out = new JsonGeneratorOut(g);
		codec.encode(out, instance);
		g.flush();
		return w.toString();
	}

	protected static <T> T fromJson(Codec<T, In, Out> codec, String json) throws IOException {
		var p = jsonFactory.createParser(json);
		var in = new JsonParserIn(p);
		return Objects.requireNonNull(codec.decode(in));
	}

	protected <T> void thatEqualRoundtrip(Codec<T, In, Out> codec, T instance) throws IOException {
		var json = toJson(codec, instance);
		T roundtripped = fromJson(codec, json);
		Assert.that(roundtripped).equalTo(instance);
	}
}
