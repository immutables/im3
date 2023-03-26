package io.immutables.common;

import io.immutables.meta.Null;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class Output {
	private final Appendable appendable;

	public int indents;
	private boolean indentExpected;

	public Output(Appendable appendable) {
		this.appendable = appendable;
	}

	public Output ln() {
		try {
			appendable.append('\n');
			indentExpected = true;
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void indentIfExpected() throws IOException {
		if (indentExpected) {
			indentExpected = false;
			for (int i = 0; i < indents; i++) {
				appendable.append(' ').append(' ');
			}
		}
	}

	public Output put(Aware a) {
		try {
			indentIfExpected();
			a.output(this);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void raw(char c) throws IOException {
		appendable.append(c);
	}

	public void raw(char[] buffer, int offset, int length) throws IOException {
		indentIfExpected();
		int l = offset + length;
		for (int i = offset; i < l; i++) {
			appendable.append(buffer[i]);
		}
	}

	public Output putIf(boolean condition, Object o) {
		if (condition) put(o);
		return this;
	}

	public Output put(CharSequence s) {
		try {
			indentIfExpected();
			appendable.append(s);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(char c) {
		try {
			indentIfExpected();
			appendable.append(c);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(Object o) {
		try {
			indentIfExpected();
			raw(o);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void raw(@Null Object o) throws IOException {
		switch (o) {
		case null -> {
			appendable.append(NULL_STRING);
		}
		case Aware aw -> {
			aw.output(this);
		}
		case CharSequence cs -> {
			appendable.append(cs);
		}
		case Character ch -> {
			appendable.append(ch);
		}
		case char[] ca -> {
			for (char c : ca) appendable.append(c);
		}
		default -> {
			appendable.append(o.toString());
		}
		}
	}

	public Output put(Object a0, Object a1) {
		try {
			indentIfExpected();
			raw(a0);
			raw(a1);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(Object a0, Object a1, Object a2) {
		try {
			indentIfExpected();
			raw(a0);
			raw(a1);
			raw(a2);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(Object a0, Object a1, Object a2, Object a3) {
		try {
			indentIfExpected();
			raw(a0);
			raw(a1);
			raw(a2);
			raw(a3);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(Object a0, Object a1, Object a2, Object a3, Object a4, Object... rest) {
		try {
			indentIfExpected();
			raw(a0);
			raw(a1);
			raw(a2);
			raw(a3);
			raw(a4);
			for (var r : rest) raw(r);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Interface which allows objects to more precisely or more efficiently output themselves
	 * using standard or raw output capabilities of this output object.
	 */
	public interface Aware {
		void output(Output o) throws IOException;
	}

	private static final String NULL_STRING = "!null!";
}
