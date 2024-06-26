package io.immutables.lang.syntax;

import io.immutables.lang.Source;
import io.immutables.meta.Null;
import java.nio.file.Path;
import static io.immutables.lang.syntax.Lang.VM;

public final class Unit<G> {
	private final Path path;
	private final char[] input;
	// some routines work on CharSequence rather than char[]
	// we just wrap array int CharSequence
	private final CharSequence source;
	private final GrammarVm grammar;
	private final Grammar.Production<G> target;
	private final Source.Lines lines;
	public @Null Terms terms;
	public @Null GrammarVm.Parsed<G> parsed;
	private G node;

	public Unit(Path path, char[] input, GrammarVm grammar, Grammar.Production<G> target) {
		this.path = path;
		this.input = input;
		this.source = Source.wrap(input);
		// we might create lines lazily,
		// only when we need to print diagnostic messages.
		// alternatively we could construct lines during tokenization,
		// but we do it as a separate step
		this.lines = Source.Lines.from(source);

		this.grammar = grammar;
		this.target = target;
	}

	public Path path() {
		return path;
	}

	public G node() {
		return node;
	}

	public boolean parse() {
		var tokenizer = new Tokenizer(input);
		tokenizer.tokenize();
		terms = tokenizer.terms;
		parsed = grammar.parse(terms, target);
		return parsed.ok();
	}

	public boolean ok() {
		return parsed != null && parsed.ok();
	}

	public void construct(ForLang context) {
		assert parsed != null;
		context.init(input, terms);
		node = parsed.construct(context);
	}

	public String message() {
		return parsed != null
			? parsed.message(lines, source)
			: "<not-parsed>";
	}

	public void printCodes() {
		System.err.println(VM.showCodes());
	}

	public void printTerms() {
		if (terms == null) {
			System.err.println("--NOT-TOKENIZED--");
		} else {
			System.err.println(Show.terms(input, terms));
		}
	}

	public void printProductions() {
		if (parsed != null) {
			System.err.println(Show.productions(input, terms, parsed.productions(), grammar));
		} else {
			System.err.println("--NOT-PARSED--");
		}
	}

	public void printResult() {
		if (parsed != null) {
			System.err.println(message());
		} else {
			System.err.println("--NOT-PARSED--");
		}
	}
}
