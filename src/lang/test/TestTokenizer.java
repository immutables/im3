package io.immutables.lang.test;

import io.immutables.lang.node.Term;
import io.immutables.lang.syntax.Show;
import io.immutables.lang.syntax.Tokenizer;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestTokenizer {
	@Test
	public void simplest() {
		var parser = new Tokenizer("""
			type {}
			""".toCharArray());

		parser.tokenize();

		var t = parser.terms;
		t.rewind();

		that(t.next()).is(Term.Type);
		that(t.next()).is(Term.Whitespace);
		that(t.next()).is(Term.BraceL);
		that(t.next()).is(Term.BraceR);
		that(t.next()).is(Term.Newline);

		that(t.next()).is(Term.EOF);
	}

	@Test public void numbers() {
		var parser = new Tokenizer("""
			0 3 1.11 2_18 3.14e+10 0xFfAa_08B3 0b1111_0000_1111
			""".toCharArray());

		parser.tokenize();

		var t = parser.terms;
		t.rewind();

		that(t.next()).is(Term.IntNumber);
		that(t.next()).is(Term.Whitespace);

		that(t.next()).is(Term.IntNumber);
		that(t.next()).is(Term.Whitespace);

		that(t.next()).is(Term.DecNumber);
		that(t.next()).is(Term.Whitespace);

		that(t.next()).is(Term.IntNumber);
		that(t.next()).is(Term.Whitespace);

		that(t.next()).is(Term.ExpNumber);
		that(t.next()).is(Term.Whitespace);

		that(t.next()).is(Term.HexNumber);
		that(t.next()).is(Term.Whitespace);

		that(t.next()).is(Term.BinNumber);
		that(t.next()).is(Term.Newline);

		that(t.next()).is(Term.EOF);
	}

	@Test public void aa() {
		char[] chars = "<p>{122..33}</p>".toCharArray();
		var parser = new Tokenizer(chars);
		parser.tokenize();

		var t = parser.terms;
		t.rewind();

		System.out.println(Show.terms(chars, t));
	}
}
