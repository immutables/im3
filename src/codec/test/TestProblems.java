package io.immutables.codec.test;

import io.immutables.codec.*;
import io.immutables.codec.jackson.JsonParserIn;
import io.immutables.codec.record.RecordsFactory;
import io.immutables.meta.Inline;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestProblems extends CodecFixture {
	private final Registry registry = new Registry.Builder()
			.add(new RecordsFactory())
			.build();

	record Tyu(Abc a, int i) {}
	record Abc(int a, boolean b, String c) {}
	@Inline record Li(int a) {}
	record Nu(@Null Integer i, String n) {}

	@Test public void allFieldsMissing() throws IOException {
		var codec = registry.resolve(Abc.class, Medium.Json).orElseThrow();
		var parser = jsonFactory.createParser("{}");
		var in = new JsonParserIn(parser, Problem.collectingHandler(4));
		var result = codec.decode(in);

		that(result).isNull();
		that(in.problems.list()).hasSize(3);

		var types = in.problems.list().stream()
				.map(problem -> (Type) problem.getClass()) // cast to avoid uncomposable wildcard
				.collect(Collectors.toUnmodifiableSet());

		that(types).isOf(Problem.MissingField.class);
	}

	@Test public void justTokenMismatch() throws IOException {
		var codec = registry.resolve(Abc.class, Medium.Json).orElseThrow();
		var parser = jsonFactory.createParser("[]");
		var in = new JsonParserIn(parser, Problem.collectingHandler(4));
		var result = codec.decode(in);

		that(result).isNull();
		that(in.problems.list()).hasSize(1);
		var problem = in.problems.list().get(0);
		that(problem).instanceOf(Problem.UnexpectedToken.class);
	}

	@Test public void nestingMissingField() throws IOException {
		var codec = registry.resolve(Tyu.class, Medium.Json).orElseThrow();
		var parser = jsonFactory.createParser("{i: 2, a: {a: 3, b: true}}");
		var in = new JsonParserIn(parser, Problem.collectingHandler(4));
		var result = codec.decode(in);

		that(result).isNull();
		that(in.problems.list()).hasSize(1);
		var problem = in.problems.list().get(0);
		that(problem).instanceOf(Problem.MissingField.class);
		var missingField = (Problem.MissingField) problem;
		that(missingField.at()).hasToString("$.a");
		that(missingField.field()).is("c");
		that(missingField.componentType()).same(String.class);
		that(missingField.recordType()).same(Abc.class);
	}

	@Test public void listOverflow() throws IOException {
		var codec = registry.resolve(
				Types.newParameterized(List.class, Li.class), Medium.Json).orElseThrow();
		var parser = jsonFactory.createParser("[true, true, true, true, true]");
		var in = new JsonParserIn(parser, Problem.collectingHandler(3));
		var result = codec.decode(in);
		that(result).isNull();

		that(in.problems.list()).hasSize(3);
		that(in.problems.isOverflowed()).is(true);

		var types = in.problems.list().stream()
				.map(problem -> (Type) problem.getClass()) // cast to avoid uncomposable wildcard
				.collect(Collectors.toUnmodifiableSet());

		that(types).isOf(Problem.UnexpectedToken.class);
	}

	@Test public void nullableFields() throws IOException {
		var codec = registry.resolve(Nu.class, Medium.Json).orElseThrow();
		var parser = jsonFactory.createParser("{i: null, n: null}");
		var in = new JsonParserIn(parser, Problem.collectingHandler(4));
		var result = codec.decode(in);
		that(result).isNull();

		that(in.problems.list()).hasSize(1);
		var problem = in.problems.list().get(0);
		that(problem).instanceOf(Problem.UnexpectedToken.class);
		var unexpected = (Problem.UnexpectedToken) problem;
		that(unexpected.at()).hasToString("$.n");
		that(unexpected.token()).same(Token.Null);
	}
}
