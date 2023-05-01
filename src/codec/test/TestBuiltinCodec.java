package io.immutables.codec.test;

import io.immutables.codec.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestBuiltinCodec extends CodecFixture {
	private final Registry registry = new Registry.Builder().build();

	@Test
	public void scalars() throws IOException {
		var forString = registry.resolve(String.class, Medium.Json).orElseThrow();
		var forInteger = registry.resolve(Integer.class, Medium.Json).orElseThrow();
		var forBoolean = registry.resolve(boolean.class, Medium.Json).orElseThrow();
		var forDouble = registry.resolve(double.class, Medium.Json).orElseThrow();
		var forFloat = registry.resolve(Float.class, Medium.Json).orElseThrow();
		var out = new Codecs.CaptureSimpleOut();

		forString.encode(out, "abc");
		that(out.asString()).is("abc");

		forInteger.encode(out, 42);
		that(out.asNumber().intValue()).is(42);

		forBoolean.encode(out, true);
		that(out.asBoolean()).is(true);

		forDouble.encode(out, 1.1);
		that(out.asNumber().doubleValue()).bitwiseIs(1.1);

		forFloat.encode(out, 1f); // assumes safe f->d conversion
		that(out.asNumber().doubleValue()).bitwiseIs(1d);
	}

	@Test
	public void primitiveArrays() throws IOException {
		var forBooleanArray = registry.resolve(boolean[].class, Medium.Json).orElseThrow();
		var forIntArray = registry.resolve(int[].class, Medium.Json).orElseThrow();

		boolean[] bools = {true, false, true};
		String bjson = toJson(forBooleanArray, bools);
		boolean[] bools1 = fromJson(forBooleanArray, bjson);
		assert Arrays.equals(bools, bools1);

		int[] ints = {1, 4, 5, 8};
		String ijson = toJson(forIntArray, ints);
		int[] ints1 = fromJson(forIntArray, ijson);
		assert Arrays.equals(ints, ints1);
	}

	@Test
	public void objectArray() throws IOException {
		var forStringArray = registry.resolve(String[].class, Medium.Json).orElseThrow();

		String[] arr = {"a", "b", "c"};
		String ajson = toJson(forStringArray, arr);
		String[] arr1 = fromJson(forStringArray, ajson);
		assert Arrays.equals(arr, arr1);
	}

	@Test
	public void optional() throws IOException {
		var forOptional = registry.<Optional<String>, In, Out>resolve(
			Types.newParameterized(Optional.class, String.class), Medium.Json).orElseThrow();

		String abc = "abc";
		String json = toJson(forOptional, Optional.of(abc));
		that(fromJson(forOptional, json)).isOf(abc);
	}

	@Test
	public void primitiveOptional() throws IOException {
		var forOptionalInt = registry.resolve(OptionalInt.class, Medium.Json).orElseThrow();
		var forOptionalLong = registry.resolve(OptionalLong.class, Medium.Json).orElseThrow();
		var forOptionalDouble = registry.resolve(OptionalDouble.class, Medium.Json).orElseThrow();

		thatEqualRoundtrip(forOptionalInt, OptionalInt.empty());
		thatEqualRoundtrip(forOptionalLong, OptionalLong.empty());
		thatEqualRoundtrip(forOptionalDouble, OptionalDouble.empty());

		thatEqualRoundtrip(forOptionalInt, OptionalInt.of(1));
		thatEqualRoundtrip(forOptionalLong, OptionalLong.of(2));
		thatEqualRoundtrip(forOptionalDouble, OptionalDouble.of(1.2));
	}

	@Test
	public void list() throws IOException {
		var forList = registry.<List<String>, In, Out>resolve(
			Types.newParameterized(List.class, String.class), Medium.Json).orElseThrow();

		var abc = List.of("a", "b", "c");
		String json = toJson(forList, abc);
		that(fromJson(forList, json)).isOf(abc);
	}

	@Test
	public void set() throws IOException {
		var forSet = registry.<Set<String>, In, Out>resolve(
			Types.newParameterized(Set.class, String.class), Medium.Json).orElseThrow();

		var xyz = Set.of("x", "y", "z");
		String json = toJson(forSet, xyz);
		that(fromJson(forSet, json)).hasOnly(xyz);
	}

	@Test
	public void toFromString() throws IOException {
		var registry = new Registry.Builder()
			.add(URI::toString, URI::create, URI.class)
			.build();

		var codec = registry.resolve(URI.class, Medium.Json).orElseThrow();
		thatEqualRoundtrip(codec, URI.create("https://immutables.io"));
	}

	@Test
	public void voidNull() throws IOException {
		var codec = registry.resolve(void.class, Medium.Json).orElseThrow();
		String json = toJson(codec, null);
		that(json).is("null");
		that(fromJson(codec, json)).isNull();
	}
}
