package io.immutables.lang.test;

import io.immutables.lang.syntax.*;
import java.nio.file.Path;
import static io.immutables.lang.test.BenchLang.*;

public class BenchRun {
	public static void main(String[] args) {
		var in = """
		// Comment
		type {
			abc ?= [1, 5, 2, tyu, res]
		}
		""".toCharArray();

		var unit = new Unit<>(Path.of("unknown"), in, VM, TypeBinding);

		unit.printCodes();

		unit.parse();

		unit.printTerms();
		unit.printProductions();
		unit.printResult();

		var parsed = unit.parsed;
		assert parsed != null;

		Stringify stringify = new Stringify(in, unit.terms);
		TypeNode node = parsed.construct(stringify);

		System.out.println("node.flag: " + VM.names().forTerm(node.flag));
		System.out.println("node.val: " + node.val);
	}

	static void main2(String[] args) {
		System.out.println(VM.showCodes());
		/*
		var input = """
			type  {"A B C{"X{ x }Y"}D E F"}
			""".toCharArray();
		*/
		var input = """
			type abcd1 {"A B C{"X{ x }Y"}D E F"}
			<Abc a={1} b="xxx" > Inner <br/> text &other; <>Text{ Goll "_{ * + - }_" Forbid }</>
			 ab
			 cd
			 ef</Abc>
			<></>
			x
			""".toCharArray();
		var parser = new Tokenizer(input);
		parser.tokenize();

		var s = Show.terms(input, parser.terms);
		System.out.println(s);
	}

	static class Stringify {
		private final Terms terms;
		private final char[] input;

		Stringify(char[] input, Terms terms) {
			this.input = input;
			this.terms = terms;
		}

		static String it(Grammar.Context context) {
			var stringify = (Stringify) context.delegate;
			return stringify.readAsString(context.word);
		}

		private String readAsString(long word) {
			int termRangeBegin = Productions.decodeTermRangeBegin(word);
			int termRangeEnd = Productions.decodeTermRangeEnd(word);

			int before = terms.sourcePositionBefore(termRangeBegin);
			int after = terms.sourcePositionAfter(termRangeEnd);

			return String.valueOf(input, before, after - before);
		}
	}
}
