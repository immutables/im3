package io.immutables.codec.test;

import io.immutables.codec.*;
import io.immutables.codec.record.RecordsFactory;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestRecordCodec extends CodecFixture {
	private final Registry registry = new Registry.Builder()
		.add(new RecordsFactory())
		.build();

	public record X<E, K, S>(S s, List<E> e, Set<K> k) {}
	public record U(int a, boolean b, String c) {}
	public record E(List<String> l, Optional<Integer> opt) {}

	public sealed interface Alt {
		record A(int a) implements Alt {}
		record B(String b, String c) implements Alt {}
		enum C implements Alt { W, V, U }
	}

	public sealed interface Alg<E, G> {
		record R<E, G>(E e, G g) implements Alg<E, G>{}
		record Q<E, G>(E h, G j) implements Alg<E, G>{}
	}

	@Test
	public void plainRecord() throws IOException {
		var codec = registry.resolve(U.class, Medium.Json).orElseThrow();

		var json = toJson(codec, new U(42, true, "string"));

		var u = fromJson(codec, json);

		that(u.a()).is(42);
		that(u.b()).is(true);
		that(u.c()).is("string");
	}

	@Test
	public void genericRecord() throws IOException {
		var codec = registry.<X<String, Integer, boolean[]>, In, Out>resolve(
			Types.newParameterizedType(X.class, String.class, Integer.class, boolean[].class),
			Medium.Json).orElseThrow();

		var x = new X<>(new boolean[]{true, false}, List.of("y"), Set.of(8, 7));

		var json = toJson(codec, x);
		var xx = fromJson(codec, json);
		that(xx.s()).is(xs -> xs[0] && !xs[1]);
		that(xx.e()).isOf("y");
		that(xx.k()).hasOnly(8, 7);
	}

	@Test
	public void defaultInstance() throws IOException {
		var codec = registry.resolve(E.class, Medium.Json).orElseThrow();

		var e = fromJson(codec, "{}");
		// parsing succeeds, fields empty but not null or smth
		that(e.l()).isEmpty();
		that(e.opt()).isEmpty();
	}

	@Test
	public void canSkip() throws IOException {
		var codec = registry.resolve(E.class, Medium.Json).orElseThrow();

		var e = toJson(codec, new E(List.of(), Optional.empty()));
		// skips Optional.empty
		that(e).is("{l:[]}");
	}

	@Test
	public void polymorphic() throws IOException {
		var codec = registry.resolve(Alt.class, Medium.Json).orElseThrow();

		thatEqualRoundtrip(codec, new Alt.A(9));
		thatEqualRoundtrip(codec, new Alt.B("b", "c"));
		thatEqualRoundtrip(codec, Alt.C.U);
		thatEqualRoundtrip(codec, Alt.C.W);
	}

	@Test
	public void polymorphicGeneric() throws IOException {
		var codec = registry.resolve(Types.newParameterizedType(
			Alg.class, String.class, Integer.class), Medium.Json).orElseThrow();

		thatEqualRoundtrip(codec, new Alg.R<>("e", 1));
		thatEqualRoundtrip(codec, new Alg.Q<>("h", 2));
	}

	@Test
	public void enums() throws IOException {
		var codec = registry.resolve(Alt.C.class, Medium.Json).orElseThrow();

		thatEqualRoundtrip(codec, Alt.C.U);
		thatEqualRoundtrip(codec, Alt.C.V);
		that(toJson(codec, Alt.C.W)).is("\"W\"");
	}
}
