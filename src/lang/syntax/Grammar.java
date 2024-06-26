package io.immutables.lang.syntax;

import io.immutables.meta.Null;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Grammar {
	void term(String literal, short term);

	void ignore(short term);

	<T> ProductionDefine<T> production(Production<T> p);

	<T> ProductionDefine<T> ephemeral(Ephemeral<T> e);

	<T> AbstractProductionAlternative<T> production(Alternatives<T> a);

	interface AbstractProductionAlternative<T> {
		AbstractProductionAlternative<T> or(NodeProduction<? extends T> p);
	}

	interface ProductionAlternative<T> {
		ProductionAlternative<T> or(Object... elements);
	}

	interface ProductionDefine<T> {
		void is(Object... elements);
		ProductionAlternative<T> or(Object... elements);
	}

	interface Factory {
		<T> Alternatives<T> alternatives();
		<T> Production<T> production(Supplier<T> create);
		<T> Production<T> production(Function<Context, T> create);
		<T, F> Production<T> production(Function<Context, F> create, Function<F, T> complete);
		<T> Ephemeral<T> ephemeral();
		<T> TermCapture<T> termCapture(TermConsumer<T> apply);
		<T, L> Part<T, L> part(BiConsumer<T, L> apply);
		GrammarVm complete(Consumer<Grammar> define);
	}

	class Context {
		public Object delegate;
		public long word;
		public int productionIndex = -1;
		//public int lastMatchedTerm = -1;
	}

	interface TermConsumer<T> {
		void accept(T node, short term);
	}

	interface ProductionKind {
/*		int id();*/
	}

	interface NodeProduction<T> extends ProductionKind {}

	interface Alternatives<T> extends NodeProduction<T> {}

	interface Production<T> extends NodeProduction<T> {}

	interface Ephemeral<T> extends ProductionKind {}

	interface TermCapture<T> extends ProductionKind {}

	interface Part<T, L> {}

	byte ZERO_OR_ONE = 0;
	byte ONLY_ONE = 1;
	byte ZERO_OR_MORE = 2;
	byte ONE_OR_MORE = 3;

	record MatchTerm(byte quantifier, Object term) {}

	record MatchNotTerm(Object term) {}

	record MatchProduction(@Null Part<?, ?> part, byte quantifier, ProductionKind production) {}

	record MatchInlineEphemeral(byte quantifier, Object[] elements) {}

	record LastMatched(Object term) {}

	record SureMatch() {}

	record MatchOneOfTerms(@Null TermCapture<?> production, Object... terms) {}

	// This might not be needed if we fix how our error reporting works
	SureMatch sure = new SureMatch();

	static MatchOneOfTerms oneOf(TermCapture<?> production, Object... terms) {
		return new MatchOneOfTerms(production, terms);
	}

	static MatchOneOfTerms oneOf(Object... terms) {
		return new MatchOneOfTerms(null, terms);
	}

	static MatchNotTerm not(String term) {
		return new MatchNotTerm(term);
	}

	static MatchNotTerm not(short term) {
		return new MatchNotTerm(term);
	}

	static LastMatched last(String term) {
		return new LastMatched(term);
	}

	static LastMatched last(short term) {
		return new LastMatched(term);
	}

	static MatchTerm opt(String term) {
		return new MatchTerm(ZERO_OR_ONE, term);
	}

	static MatchTerm opt(short term) {
		return new MatchTerm(ZERO_OR_ONE, term);
	}

	static MatchProduction any(Ephemeral<?> production) {
		return new MatchProduction(null, ZERO_OR_MORE, production);
	}

	static MatchProduction opt(Ephemeral<?> production) {
		return new MatchProduction(null, ZERO_OR_ONE, production);
	}

	static MatchTerm any(short term) {
		return new MatchTerm(ZERO_OR_MORE, term);
	}

	static MatchTerm any(String term) {
		return new MatchTerm(ZERO_OR_MORE, term);
	}

	static MatchTerm more(short term) {
		return new MatchTerm(ONE_OR_MORE, term);
	}

	static MatchTerm more(String term) {
		return new MatchTerm(ONE_OR_MORE, term);
	}

	static <L> MatchProduction one(Part<?, ? super L> part, NodeProduction<L> production) {
		return new MatchProduction(part, ONLY_ONE, production);
	}

	static <L> MatchProduction opt(Part<?, ? super L> part, NodeProduction<L> production) {
		return new MatchProduction(part, ZERO_OR_ONE, production);
	}

	static MatchProduction opt(NodeProduction<?> production) {
		return new MatchProduction(null, ZERO_OR_ONE, production);
	}

	static <L> MatchProduction any(Part<?, ? super L> part, NodeProduction<L> production) {
		return new MatchProduction(part, ZERO_OR_MORE, production);
	}

	static MatchProduction any(NodeProduction<?> production) {
		return new MatchProduction(null, ZERO_OR_MORE, production);
	}

	static <L> MatchProduction more(Part<?, ? super L> part, NodeProduction<L> production) {
		return new MatchProduction(part, ONE_OR_MORE, production);
	}

	static MatchProduction more(NodeProduction<?> production) {
		return new MatchProduction(null, ONE_OR_MORE, production);
	}

	static MatchProduction more(Ephemeral<?> production) {
		return new MatchProduction(null, ONE_OR_MORE, production);
	}

	static Object opt(Object e0) {
		if (e0 instanceof ProductionKind p) {
			return new MatchProduction(null, ZERO_OR_ONE, p);
		}
		return new MatchInlineEphemeral(ZERO_OR_ONE, new Object[]{e0});
	}

	static MatchInlineEphemeral opt(Object e0, Object e1, Object... es) {
		return new MatchInlineEphemeral(ZERO_OR_ONE, concat(e0, e1, es));
	}

	static MatchInlineEphemeral any(Object e0, Object e1, Object... es) {
		return new MatchInlineEphemeral(ZERO_OR_MORE, concat(e0, e1, es));
	}

	static MatchInlineEphemeral more(Object e0, Object e1, Object... es) {
		return new MatchInlineEphemeral(ONE_OR_MORE, concat(e0, e1, es));
	}

	private static Object[] concat(Object e0, Object e1, Object[] es) {
		var all = new Object[es.length + 2];
		all[0] = e0;
		all[1] = e1;
		System.arraycopy(es, 0, all, 2, es.length);
		return all;
	}
}
