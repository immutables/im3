package io.immutables.that.test;

import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestThat {
	@Test
	public void assertString() {
		that("a").is(new String("a"));
	}

	@Test
	public void assertSameString() {
		that("a").just().same("a");
	}

	@Test
	public void assertStringPassing() {
		that("a").is("a");
		that("").isEmpty();
	}

	@Test(expected = AssertionError.class)
	public void assertStringEqual() {
		that("a").is("b");
	}

	@Test(expected = AssertionError.class)
	public void assertStringEmpty() {
		that("a").isEmpty();
	}
}
