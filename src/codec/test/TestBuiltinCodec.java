package io.immutables.codec.test;

import io.immutables.codec.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestBuiltinCodec extends CodecTesting {
	@Test
	public void scalars() throws IOException {
		var forString = newRegistry().resolve(String.class, Medium.Json).orElseThrow();
		var forInteger = newRegistry().resolve(Integer.class, Medium.Json).orElseThrow();
		var forBoolean = newRegistry().resolve(boolean.class, Medium.Json).orElseThrow();
		var forDouble = newRegistry().resolve(double.class, Medium.Json).orElseThrow();
		var forFloat = newRegistry().resolve(Float.class, Medium.Json).orElseThrow();
		var out = new CapureValueOut();

		forString.encode(out, "abc");
		that(out.stringValue).is("abc");

		forInteger.encode(out, 42);
		that(out.intValue).is(42);

		forBoolean.encode(out, true);
		that(out.booleanValue).is(true);

		forDouble.encode(out, 1.1);
		that(out.doubleValue).bitwiseIs(1.1);

		forFloat.encode(out, 1f); // safe f->d conversion
		that(out.doubleValue).bitwiseIs(1d);
	}

	@Test
	public void primitiveArrays() throws IOException {
		var forBooleanArray = newRegistry().resolve(boolean[].class, Medium.Json).orElseThrow();
		var forIntArray = newRegistry().resolve(int[].class, Medium.Json).orElseThrow();

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
		var forStringArray = newRegistry().resolve(String[].class, Medium.Json).orElseThrow();

		String[] arr = {"a", "b", "c"};
		String ajson = toJson(forStringArray, arr);
		String[] arr1 = fromJson(forStringArray, ajson);
		assert Arrays.equals(arr, arr1);
	}

	@Test
	public void optional() throws IOException {
		var forOptional = newRegistry().<Optional<String>, In, Out>resolve(
			Types.newParameterizedType(Optional.class, String.class), Medium.Json).orElseThrow();

		String abc = "abc";
		String json = toJson(forOptional, Optional.of(abc));
		that(fromJson(forOptional, json)).isOf(abc);
	}

	private static Registry newRegistry() {
		return new Registry.Builder().build();
	}
}
