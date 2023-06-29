package io.immutables.stencil;

/**
 * @deprecated until deleted or un-deprecated
 */
@Deprecated
public class JavaStencil extends Stencil.Raw {
	private final Comments comments = new Comments();

	public void comments(String content) {
		comments.lines(COMMENT_LINE).content(content);
	}

	public void comments(String... lines) {
		comments.lines(COMMENT_LINE).content(lines);
	}

	public Comments.Content comments() {
		return comments.lines(COMMENT_LINE);
	}

	public Comments.Content block() {
		return comments.block("/*", " * ", " */");
	}

	public Comments.Content doc() {
		return comments.block("/**", " * ", " */");
	}

	public void doc(Runnable run) {
		doc().content(run);
	}

	public CharSequence literal(String string) {
		return Literals.string(string);
	}

	public void braces(Runnable run) {
		var out = out();
		out.put('{').ln();
		out.indents++;

		run.run();

		out.indents--;
		out.ifln();
		out.put('}').ln();
	}

	static final String COMMENT_LINE = "// ";
}
