package io.immutables.lang.test;

import io.immutables.lang.syntax.Terms;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestTerms {
	@Test
	public void put() {
		Terms terms = new Terms();

		that(terms.count()).is(0);
		that(terms.index()).is(-1);

		terms.put(Term1, 3);

		that(terms.sourcePositionBefore()).is(0);
		that(terms.sourcePositionAfter()).is(3);
		that(terms.current()).is(Term1);

		that(terms.count()).is(1);

		terms.put(Term2, 5);
		that(terms.count()).is(2);

		terms.put(Term1, 7);
		that(terms.count()).is(3);
		that(terms.current()).is(Term1);

		terms.put(Term3, 11);
		that(terms.count()).is(4);
		that(terms.sourcePositionBefore()).is(7);
		that(terms.sourcePositionAfter()).is(11);
		that(terms.current()).is(Term3);
	}

	@Test
	public void seek() {
		var terms = sampleTerms();

		that(terms.seek(-1)).is(SOI);
		that(terms.current()).is(SOI);

		that(terms.seek(0)).is(Term1);
		that(terms.current()).is(Term1);

		that(terms.seek(4)).is(EOF);
		that(terms.current()).is(EOF);

		that(terms.seek(2)).is(Term1);
		that(terms.current()).is(Term1);

		that(terms.seek(1)).is(Term2);
		that(terms.current()).is(Term2);

		that(terms.seek(3)).is(Term3);
		that(terms.current()).is(Term3);

		that(() -> terms.seek(-2)).thrown(IndexOutOfBoundsException.class);
		that(() -> terms.seek(5)).thrown(IndexOutOfBoundsException.class);
	}

	@Test
	public void rewindAdvance() {
		var terms = sampleTerms();
		that(terms.index()).is(3);
		that(terms.count()).is(4);

		terms.rewind();
		that(terms.index()).is(-1);
		that(terms.current()).is(SOI);
		that(terms.count()).is(4);

		that(terms.next()).is(Term1);
		that(terms.index()).is(0);
		that(terms.sourcePositionBefore()).is(0);
		that(terms.sourcePositionAfter()).is(3);

		that(terms.next()).is(Term2);
		that(terms.index()).is(1);
		that(terms.sourcePositionBefore()).is(3);
		that(terms.sourcePositionAfter()).is(5);

		that(terms.next()).is(Term1);
		that(terms.index()).is(2);
		that(terms.sourcePositionBefore()).is(5);
		that(terms.sourcePositionAfter()).is(7);

		that(terms.next()).is(Term3);
		that(terms.index()).is(3);
		that(terms.sourcePositionBefore()).is(7);
		that(terms.sourcePositionAfter()).is(11);

		that(terms.next()).is(EOF);
		that(terms.next()).is(EOF);
		that(terms.next()).is(EOF);
	}

	private static Terms sampleTerms() {
		var terms = new Terms();
		terms.put(Term1, 3);
		terms.put(Term2, 5);
		terms.put(Term1, 7);
		terms.put(Term3, 11);
		return terms;
	}

	private static final short Term1 = 1;
	private static final short Term2 = 2;
	private static final short Term3 = 3;
	private static final short EOF = -1;
	private static final short SOI = -2;
}
