package io.immutables.codec.test;

import io.immutables.codec.AtPath;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestPath {
	final AtPath path = new AtPath.ElementAt(
			new AtPath.FieldOf(
					new AtPath.FieldOf(
							new AtPath.ElementAt(
									AtPath.Root.Root, 1), "tuv"), "abc"), 3);

	@Test public void stringify() {
		that(path).hasToString("$[1].tuv.abc[3]");
		that(AtPath.Root.Root).hasToString("$");
	}

	@Test public void unwind() {
		var l = path.unwind();
		that(l).hasSize(5);
		that(l.get(0)).same(AtPath.Root.Root);
		that(l.get(1) instanceof AtPath.ElementAt e && e.index() == 1).is(true);
		that(l.get(2) instanceof AtPath.FieldOf e && e.name().equals("tuv")).is(true);
		that(l.get(3) instanceof AtPath.FieldOf e && e.name().equals("abc")).is(true);
		that(l.get(4) instanceof AtPath.ElementAt e && e.index() == 3).is(true);
	}
}
