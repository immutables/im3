
package io.immutables.stencil.test;

import io.immutables.stencil.Glob;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class GlobTest {
	@Test public void anyDirsFull() {
		var g = Glob.of("**");
		that().is(g.matches(""));
		that().is(g.matches("/"));
		that().is(g.matches("/a"));
		that().is(g.matches("/a/b/c"));
	}

	@Test public void anyDirsPrefix() {
		var g = Glob.of("**/a");
		that().not(g.matches(""));
		that().not(g.matches("/"));
		that().not(g.matches("b"));

		that().is(g.matches("a"));
		that().is(g.matches("/a"));
		that().is(g.matches("/b/c/a"));
	}

	@Test public void anyDirsSuffix() {
		var g = Glob.of("a/**");
		that().not(g.matches(""));
		that().not(g.matches("/"));
		that().not(g.matches("a"));
		that().not(g.matches("/a"));
		that().not(g.matches("/b/c/a"));

		that().is(g.matches("a/b"));
		that().is(g.matches("a/b/c"));
		that().is(g.matches("/a/b"));
	}

	@Test public void anyDirs() {
		var g = Glob.of("a/b/**/c");
		that().not(g.matches(""));
		that().not(g.matches("/"));
		that().not(g.matches("a/b/c/d"));

		that().is(g.matches("a/b/c"));
		that().is(g.matches("a/b/d/c"));
		that().is(g.matches("/a/b/d/c"));
		that().is(g.matches("a/b/d/e/f/c"));
	}

	@Test public void any() {
		var g = Glob.of("/a*c");
		that().not(g.matches("/"));
		that().not(g.matches("ac"));
		that().not(g.matches("a/c"));


		that().is(g.matches("/ac"));
		that().is(g.matches("/aaacc"));
		that().is(g.matches("/addddc"));
	}

	@Test public void ones() {
		var g = Glob.of("**/?.jav?");
		that().not(g.matches("a/.java"));
		that().not(g.matches(".java"));


		that().is(g.matches("a.java"));
		that().is(g.matches("a/b.javb"));
		that().is(g.matches("a/b/c.javc"));
		that().is(g.matches("/a/b.javd"));
	}

	@Test public void middle() {
		var g = Glob.of("**/a?c*d/**");
		that().not(g.matches("aabcdd"));
		that().not(g.matches("t/abc/u"));

		that().is(g.matches("abcd"));
		that().is(g.matches("avcad/t/u"));
		that().is(g.matches("t/avcacdd/u"));
		that().is(g.matches("w/t/auccd/v/j"));
	}
}
