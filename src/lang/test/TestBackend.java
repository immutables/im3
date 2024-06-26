package io.immutables.lang.test;

import io.immutables.lang.back.Output;
import java.io.StringWriter;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestBackend {
	@Test public void test() {}

	@Test public void output() {
		var w = new StringWriter();

		var o = new Output(w);
		o.put('_').ln();
		o.put("a");
		o.indents = 2;
		o.put("b", "c").ln();
		o.put("x", "y", "z").ln();
		o.indents = 0;
		o.put("%", "#", "@").ln();

		that(w.toString()).is("""
		_
		abc
		    xyz
		%#@
		""");
	}
}
