package io.immutables.stencil.test;

import io.immutables.stencil.Output;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestOutput {
	@Test public void output() throws IOException {
		var w = new StringWriter();

		var o = new Output();
		o.put('_').ln();

		o.raw.append("atu", 0, 1);

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
