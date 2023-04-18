package io.immutables.regres.test;

import io.immutables.codec.Medium;
import io.immutables.codec.Registry;
import io.immutables.codec.record.RecordsFactory;
import io.immutables.codec.test.CodecFixture;
import io.immutables.regres.Codecs;
import io.immutables.regres.Jsons;
import java.io.IOException;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestCodec extends CodecFixture {
	private final Registry registry = new Registry.Builder()
		.add(new RecordsFactory())
		.add(Codecs.jsonsFactory(jsonFactory), Medium.Any, Jsons.class)
		.build();

	public record Abc(
		Jsons<Xyz> j,
		double u
	) {}

	public record Xyz(int a, boolean b) {}

	@Test
	public void jsons() throws IOException {
		var codec = registry.resolve(Abc.class, Medium.Json).orElseThrow();
		var abc = new Abc(new Jsons<>(new Xyz(8, true)), 1.0);

		that(toJson(codec, abc)).is("{j:\"{a:8,b:true}\",u:1.0}");
	}
}
