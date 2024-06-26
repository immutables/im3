package io.immutables.lang.test;

import io.immutables.lang.syntax.Productions;
import org.junit.Test;
import static io.immutables.lang.syntax.Productions.*;
import static io.immutables.that.Assert.that;

public class TestProductions {
	@Test
	public void encode() {
		long l = 0;

		l = encodeProductionPart(l, 142, 384);
		l = encodeTermRange(l, 1515, 2047);
		l = encodeLength(l, 18);

		that(decodeLength(l)).is(18);
		that(decodeProduction(l)).is(142);
		that(decodePart(l)).is(384);
		that(decodeTermRangeBegin(l)).is(1515);
		that(decodeTermRangeEnd(l)).is(2047);
	}

	@Test
	public void write() {
		var ps = new Productions<Void>();
		that(ps.index()).is(-1);
		that(ps.count()).is(0);
		that(ps::current).thrown(IllegalStateException.class);

		ps.put(10);
		ps.put(20);
		ps.put(30);

		that(ps.index()).is(2);
		that(ps.count()).is(3);
		that(ps.current()).is(30);
	}

	@Test
	public void overwrite() {
		var ps = new Productions<Void>();
		that(ps.index()).is(-1);
		that(ps.count()).is(0);
		ps.put(10);
		ps.put(20);
		that(ps.index()).is(1);
		that(ps.count()).is(2);
		that(ps.current()).is(20);

		ps.rewind();
		ps.put(40);
		that(ps.index()).is(0);
		that(ps.count()).is(1);
		that(ps.current()).is(40);
	}

	@Test
	public void read() {
		var ps = new Productions<Void>();
		ps.put(10);
		ps.put(20);
		ps.put(30);

		that(ps.count()).is(3);
		that(ps.advance()).is(false);

		ps.rewind();
		that(ps.index()).is(-1);

		that(ps.advance()).is(true);
		that(ps.current()).is(10);
		that(ps.index()).is(0);

		that(ps.advance()).is(true);
		that(ps.current()).is(20);
		that(ps.index()).is(1);

		that(ps.advance()).is(true);
		that(ps.current()).is(30);
		that(ps.index()).is(2);

		that(ps.advance()).is(false);
		that(ps.index()).is(3);
		// index can be == length, but cannot be advanced further
		that(ps.advance()).is(false);
		that(ps.index()).is(3);
	}
}
