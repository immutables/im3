package io.immutables.lang.test;

import io.immutables.lang.node.Identifier;
import io.immutables.lang.node.Operators;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestIdentifier {
	String abc = "abc";
	String dabce = "dabce";
	String xyz = "xyz";
	String bbyby = "bbyby";

	@Test
	public void pool() {
		var p = new Identifier.Pool();

		Identifier interned = p.interned(abc.toCharArray(), 0, abc.length());
		Identifier interned2 = p.interned(dabce.toCharArray(), 1, abc.length());

		that(interned).same(interned2);
		that(interned).hasToString("abc");
	}

	@Test
	public void page() {
		var p = new Identifier.Pool();

		p.interned(abc.toCharArray(), 0, abc.length());
		p.interned(xyz.toCharArray(), 0, xyz.length());
		p.interned(dabce.toCharArray(), 1, abc.length());
		p.interned(bbyby.toCharArray(), 1, bbyby.length() - 1);

		that(p.values().stream().map(Object::toString).toList())
			.hasOnly("abc", "xyz", "byby");
	}

	@Test
	public void operator() {
		that(Operators.Plus).hasToString("+");
		that(Operators.Multiply).hasToString("*");
		that(Operators.ArrowRight).hasToString("->");
		that(Operators.Decrement).hasToString("--");
	}
}
