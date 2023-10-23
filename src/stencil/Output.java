package io.immutables.stencil;

import io.immutables.meta.Null;
import io.immutables.meta.SkippableReturn;

public class Output {
	public final StringBuilder raw = new StringBuilder();

	public int indents;

	// these expected to not have newlines,
	// otherwise indentation will be broken
	public @Null String indentStep;
	public @Null String indentPrefix;
	public @Null String nullString;

	private boolean indentExpected = false;
	private boolean shouldSkipBlankLine = false;
	private int lineStartAt = 0;
	private int trimWhitespaceAfter = NOPE;

	@SkippableReturn
	public Output ln() {
		return put('\n');
	}

	@SkippableReturn
	public Output ifln() {
		if (!indentExpected) ln();
		return this;
	}

	@SkippableReturn
	public Output putIf(boolean condition, Object o) {
		if (condition) put(o);
		return this;
	}

	private void indentIfExpected() {
		if (indentExpected) {
			indentExpected = false;
			indent(raw);
		}
	}

	/** makes next newline remove previous whitespace line */
	@SkippableReturn
	public Output deline() {
		shouldSkipBlankLine = true;
		return this;
	}

	@SkippableReturn
	public Output put(char c) {
		if (trimWhitespaceAfter > NOPE) {
			// if current character is a whitespace, dropping and return
			if (isSimpleWhitespace(c)) return this;
			// non-whitespace, so we drop trim mode and continue
			trimWhitespaceAfter = NOPE;
		}
		if (c == '\n') {
			// we don't put indentation after newline,
			// we request indentation performed on next char
			indentExpected = true;
			if (!skipBlankLine()) {
				raw.append(c);
				lineStartAt = raw.length();
			}
			return this;
		} else {
			indentIfExpected();
			raw.append(c);
		}
		return this;
	}

	boolean skipBlankLine() {
		trimWhitespaceAfter = NOPE;//FIXME ??? should we drop it here

		if (shouldSkipBlankLine) {
			shouldSkipBlankLine = false;
			// from the beginning of the previous line (before we updated that value)
			// up until newline we've just put
			for (int i = lineStartAt; i < raw.length(); i++) {
				char b = raw.charAt(i);
				if (!isSimpleWhitespace(b)) {
					// a single non-blank assigns new line boundary and we bail out
					return false;
				}
			}
			// all blanks here, we skip and clear that previous line
			raw.setLength(lineStartAt);
			return true;
		}
		return false;
	}

	public void trimWhitespaceAfter() {
		if (trimWhitespaceAfter == NOPE) {
			trimWhitespaceAfter = raw.length();
		}
	}

	//[>-]
	// ab/__
	// 01 23
	//     i
	public void trimWhitespaceBefore() {
		int len = raw.length(), i = len;
		int min = trimWhitespaceAfter > NOPE ? trimWhitespaceAfter : 0;
		// clearing trimWhitespaceAfter if it was set, anyway
		trimWhitespaceAfter = NOPE;
		for (; i > min; i--) {
			char c = raw.charAt(i - 1);
			if (!isSimpleWhitespace(c)) break;
		}
		if (i != len) {
			raw.setLength(i);
			// need to reestablish lineStartAt,
			lineStartAt = 0; // if not found, we have to have good start
			// using 0, not min, last relevant newline might have been before
			for (; i > 0; i--) {
				char c = raw.charAt(i - 1);
				if (c == '\n') {
					lineStartAt = i;
					break;
				}
			}
		}
	}

	/** only spaces, tabs, or newline do not consider any other whitespace for this purpose. */
	static boolean isSimpleWhitespace(char c) {
		return switch (c) {
			case ' ', '\t', '\n' -> true;
			//TODO need these?
			//case '\r', '\f' -> true;
			default -> false;
		};
	}

	@SkippableReturn
	public Output put(@Null Object o) {
		// we use indentExpected only before raw operations here,
		// otherwise it will be done in put(char)
		if (o == null) {
			indentIfExpected();
			raw.append(nullString);
		} else if (o instanceof SelfFormatting aw) {
			aw.output(this);
		} else if (o instanceof Character ch) {
			put(ch.charValue());
		} else if (o instanceof String str) {
			if (trimWhitespaceAfter > NOPE || str.indexOf('\n') >= 0) {
				// resort to char by char. so we process newlines
				// or trim whitespace correctly
				for (int i = 0; i < str.length(); i++) {
					put(str.charAt(i));
				}
			} else {
				indentIfExpected();
				raw.append(str);
			}
		} else if (o instanceof CharSequence cs) {
			for (int i = 0; i < cs.length(); i++) {
				put(cs.charAt(i));
			}
		} else if (o instanceof char[] ca) {
			for (char c : ca) put(c);
		} else {
			put(o.toString()); // recursive
		}
		return this;
	}

	@SkippableReturn
	public Output put(Object a0, Object a1) {
		return put(a0).put(a1);
	}

	@SkippableReturn
	public Output put(Object a0, Object a1, Object a2) {
		return put(a0).put(a1).put(a2);
	}

	@SkippableReturn
	public Output put(Object a0, Object a1, Object a2, Object a3) {
		return put(a0).put(a1).put(a2).put(a3);
	}

	@SkippableReturn
	public Output putAll(Object[] all) {
		for (var r : all) put(r);
		return this;
	}

	@SkippableReturn
	public Output put(Object a0, Object a1, Object a2, Object a3, Object a4, Object... rest) {
		put(a0).put(a1).put(a2).put(a3).put(a4);
		for (var r : rest) put(r);
		return this;
	}

	public StringBuilder currentLine() {
		StringBuilder b = new StringBuilder();
		if (lineStartAt == raw.length()) {
			// these two conditions: (1) lineStartAt == raw.length() (2) indentExpected
			// can be always true at the same time,
			// I just speculate that these might be checked separately,
			// be we could replace (2) with assertion
			if (indentExpected) indent(b);
		} else {
			b.append(raw, lineStartAt, raw.length());
		}
		return b;
	}

	public StringBuilder currentIndent() {
		StringBuilder b = new StringBuilder();
		indent(b);
		return b;
	}

	private void indent(StringBuilder builder) {
		if (indentPrefix != null) {
			builder.append(indentPrefix);
		}
		if (indentStep != null) {
			for (int i = 0; i < indents; i++) {
				builder.append(indentStep);
			}
		} else {
			for (int i = 0; i < indents; i++) {
				builder.append(' ');
			}
		}
	}

	@Override
	public String toString() {
		skipBlankLine();
		return raw.toString();
	}

	/**
	 * Interface which allows objects to more precisely or more efficiently output themselves
	 * using standard or raw output capabilities of this output object.
	 */
	public interface SelfFormatting {
		void output(Output output);
	}

	private static final int NOPE = -1;
}
