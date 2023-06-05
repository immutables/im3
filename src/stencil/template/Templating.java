package io.immutables.stencil.template;

import java.util.List;
import java.util.Optional;

interface Templating {

	interface Element {
		int from();
		int to();
	}

	interface Content {
		int from();
		int to();
	}

	interface Verbatim extends Content {}

	interface Directive extends Content {}

	interface HasContent {
		List<Content> content();
	}

	record NonTemplate(int from, int to, StringBuilder content) implements Element {}

	record Import(int from, int to, StringBuilder content) implements Element {}

	record Comment(int from, int to, StringBuilder content) implements Element, Content {}

	record TrimWhitespace(int from, int to, Direction direction)
		implements Content, Directive {
		enum Direction {
			After, Before
		}
	}

	record Definition(int from, int to, String identifier, StringBuilder signature,
		List<String> parameterNames,
		List<Content> content) implements Element, HasContent {}

	record Fragment(int from, int to, StringBuilder content) implements Verbatim {}

	record Newline(int from, int to) implements Verbatim {}

	record Expression(int from, int to, ExpressionContent expression)
		implements Directive {}

	record If(int from, int to, List<Then> then, Optional<Else> otherwise)
		implements Directive {

		public record Then(ExpressionContent condition, List<Content> content)
			implements HasContent {}

		public record Else(List<Content> content)
			implements HasContent {}

		public record Compact(int from, int to, Then then, Optional<Else> otherwise)
			implements Directive {}
	}

	record Let(int from, int to, String identifier, List<Content> content)
		implements Directive, HasContent {}

	record Spread(int from, int to, ExpressionContent expression, List<Content> content)
		implements Directive, HasContent {}

	record Case(int from, int to, ExpressionContent expression, List<Either> either,
		Optional<Else> otherwise) implements Directive {

		record Either(int from, int to, ExpressionContent expression, List<Content> content)
			implements HasContent {}

		record Else(int from, int to, List<Content> content) implements HasContent {}
	}

	record BlockExpression(int from, int to, ExpressionContent expression,
		List<Content> content) implements Directive, HasContent {}

	record For(int from, int to, List<Clause> clauses, List<Content> content)
		implements Directive, HasContent {

		public sealed interface Clause {}
		public record Assign(String identifier, ExpressionContent expression) implements Clause {}
		public record Each(String identifier, ExpressionContent expression) implements Clause {}
		public record Predicate(ExpressionContent expression) implements Clause {}
	}
}

