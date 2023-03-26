package io.immutables.common.test;

import io.immutables.common.Output;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestOutput {
	@Test public void output() throws IOException {
		var w = new StringWriter();

		var o = new Output(w);
		o.put('_').ln();

		char[] atuChars = "atu".toCharArray();
		o.raw(atuChars, 0, 1);

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
