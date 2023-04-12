package io.immutables.codec.test;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestRegistry {
	@Test(expected = IllegalArgumentException.class)
	public void noAnyForResolving() {
		newRegistry().resolve(String.class, Medium.Any);
	}

	@Test
	public void registryNoBuiltins() {
		that(newRegistry().resolve(String.class, Medium.Json))
			.isPresent();

		that(new Registry.Builder()
			.noBuiltin()
			.build()
			.resolve(String.class, Medium.Json))
			.isEmpty();
	}

	@Test
	public void specificAndAnyMediumFactories() throws IOException {
		var c1 = codec();
		var c2 = codec();
		var c3 = codec();
		var c4 = codec();
		var c5 = codec();

		var registry = new Registry.Builder()
			.noBuiltin()
			.add((t, r, m, l) -> c1)
			.add((t, r, m, l) -> c2, Medium.Json)
			.add((t, r, m, l) -> c3, Medium.Any, void.class, int.class)
			.add((t, r, m, l) -> c4, Medium.Json, void.class)
			.add((t, r, m, l) -> c5, MediumOther, double.class)
			.build();

		that((Object) registry.resolve(long.class, MediumOther).orElseThrow())
			.same(c1);
		that((Object) registry.resolve(long.class, Medium.Json).orElseThrow())
			.same(c2);
		that((Object) registry.resolve(int.class, Medium.Json).orElseThrow())
			.same(c3);
		that((Object) registry.resolve(void.class, MediumOther).orElseThrow())
			.same(c3);
		that((Object) registry.resolve(void.class, Medium.Json).orElseThrow())
			.same(c4);
		that((Object) registry.resolve(double.class, MediumOther).orElseThrow())
			.same(c5);
	}

	private static Codec<Object, In, Out> codec() {
		return new Codec<>() {
			public void encode(Out out, Object instance) {}

			@Null public Object decode(In in) {return null;}
		};
	}

	private static Registry newRegistry() {
		return new Registry.Builder().build();
	}

	static final Medium<In, Out> MediumOther = new Medium<>() {
		public String toString() {return "Sample";}
	};
}
