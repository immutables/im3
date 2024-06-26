package io.immutables.lang.syntax;

import io.immutables.lang.node.Identifier;
import io.immutables.lang.node.Node;
import io.immutables.lang.node.SourceSpan;

public class ForLang {
	char[] input;
	Terms terms;

	public void init(char[] input, Terms terms) {
		this.input = input;
		this.terms = terms;
	}

	public Identifier.Pool identifiers = new Identifier.Pool();
	SourceSpan.Preallocated spans = new SourceSpan.Preallocated();

	public static ForLang from(Grammar.Context context) {
		return (ForLang) context.delegate;
	}

	@Deprecated // just unused?
	public static <N extends Node> N within(Grammar.Context context, N node) {
		node.productionIndex = context.productionIndex;
		return node;
	}

	public static SourceSpan span(Grammar.Context c) {
		var context = (ForLang) c.delegate;

		var termBeginIndex = Productions.decodeTermRangeBegin(c.word);
		var termEndIndex = Productions.decodeTermRangeEnd(c.word);

		var offset = context.terms.sourcePositionBefore(termBeginIndex);
		var length = context.terms.sourcePositionAfter(termEndIndex) - offset;

		var span = context.spans.create(context.input, offset, length);
		/*
		span.termBegin = context.terms.at(termBeginIndex);
		span.termEnd = context.terms.at(termEndIndex);
		*/
		return span;
	}

	public static Identifier identifer(Grammar.Context c) {
		var context = (ForLang) c.delegate;

		var termBegin = Productions.decodeTermRangeBegin(c.word);
		var termEnd = Productions.decodeTermRangeEnd(c.word);

		var offset = context.terms.sourcePositionBefore(termBegin);
		var length = context.terms.sourcePositionAfter(termEnd) - offset;

		return context.identifiers.interned(context.input, offset, length);
	}

	// TODO should we change nudge convention from (offsetDelta, lengthDelta)
	// to (beginDelta, endDelta). If we do this it will be easier to image in head as
	// we will not have to force to include offset delta into length delta every time
	public static Identifier identifer(Grammar.Context c, int offsetDelta, int lengthDelta) {
		var context = (ForLang) c.delegate;

		var termBegin = Productions.decodeTermRangeBegin(c.word);
		var termEnd = Productions.decodeTermRangeEnd(c.word);

		var offset = context.terms.sourcePositionBefore(termBegin) + offsetDelta;
		var length = context.terms.sourcePositionAfter(termEnd) - offset + lengthDelta;

		return context.identifiers.interned(context.input, offset, length);
	}
}
