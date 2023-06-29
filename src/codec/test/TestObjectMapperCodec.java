package io.immutables.codec.test;

import io.immutables.codec.Medium;
import io.immutables.codec.Registry;
import io.immutables.codec.jackson.ObjectMapperCodecs;
import io.immutables.meta.Null;
import java.io.IOException;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestObjectMapperCodec extends CodecFixture {
	private final Registry registry = new Registry.Builder()
		.add(new ObjectMapperCodecs(new ObjectMapper(jsonFactory)))
		.build();

	public static class PlainStruct {
		public int a;
		public @Null String b;

		public boolean equals(Object obj) {
			return obj instanceof PlainStruct ps
				&& ps.a == a && Objects.equals(ps.b, b);
		}
	}

	@Test
	public void delegateToObjectMapper() throws IOException {
		var codec = registry.resolve(PlainStruct.class, Medium.Json).orElseThrow();
		var p = new PlainStruct();
		p.a = 7;
		p.b = "Y";
		thatEqualRoundtrip(codec, p);
	}
}
