package io.immutables.lang.syntax;

import io.immutables.lang.Source;
import io.immutables.lang.node.Term;

public final class Show {
	private Show() {}

	public static String terms(char[] input, Terms terms) {
		StringBuilder b = new StringBuilder();

		terms.rewind();

		Source.Lines lines = Source.Lines.from(input);
		var source = Source.wrap(input);

		for (int t; (t = terms.next()) != Term.EOF;) {
			int before = terms.sourcePositionBefore();
			int after = terms.sourcePositionAfter();

			if (after > input.length) {
				System.out.println("BOGUS " + after + " > " + input.length);
				after = input.length;
			}
			String fragment = String.valueOf(input, before, after - before);

			var about = Term.Info.about((short) t);
			b.append(String.format("%04d—%04d +%04d |", before, after, after - before))
				.append(fragment.replace("\n", "\u23CE\n                |"))
				.append("|\t\t\t\t")
				.append(about.name())
				.append("—")
				.append(about.kind())
				.append(":\t\t ")
				.append(about.symbol())
				.append('\n');
		}
		return b.toString();
	}

	public static String productions(char[] input, Terms terms, Productions<?> productions, GrammarVm vm) {
		StringBuilder b = new StringBuilder();

		Source.Lines lines = Source.Lines.from(input);
		var source = Source.wrap(input);

		productions.rewind();

		var names = vm.names();

		while (productions.advance()) {
			int position = productions.index();
			long word = productions.current();

			int nextIncrement = Productions.decodeLength(word);
			int nextSibling = position + nextIncrement;
			int production = Productions.decodeProduction(word);
			int part = Productions.decodePart(word);

			int termIndexBegin = Productions.decodeTermRangeBegin(word);
			int termIndexEnd = Productions.decodeTermRangeEnd(word);
			int termBegin = terms.sourcePositionBefore(termIndexBegin);
			int termEnd = terms.sourcePositionAfter(termIndexEnd);

			var range = Source.Range.of(lines.get(termBegin), lines.get(termEnd));
			var label = names.forPart(part) + ":" + names.forProduction(production);

			b.append(String.format("%04d—%04d +%04d | ", position, nextSibling, nextIncrement))
				.append(label)
				.append(" ".repeat(40 - label.length()))
				.append(" |")
				.append(String.valueOf(range.get(source))
					.replace("\n", "\n" + " ".repeat(59) + "|"));//l1 < 0 ? "????" :

			b.append('\n');
		}

		return b.toString();
	}

	public static String showQuantifier(int quantifier) {
		return switch (quantifier) {
			case Grammar.ONLY_ONE -> "";
			case Grammar.ONE_OR_MORE -> "+";
			case Grammar.ZERO_OR_ONE -> "?";
			case Grammar.ZERO_OR_MORE -> "*";
			default -> throw new AssertionError("Unsupported quantifier value: " + quantifier);
		};
	}
}
