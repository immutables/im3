package io.immutables.lang.back;

import io.immutables.meta.NullUnknown;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class Output {
	private final Appendable appendable;

	public int indents;
	// TODO FIXME need to refactor these screenBackendKeyword / Identifier.backendKeyword mess
	public boolean screenBackendKeyword;
	private boolean indent;

	public Output(Appendable appendable) {
		this.appendable = appendable;
	}

	public Output ln() {
		try {
			appendable.append('\n');
			indent = true;
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void indentIfNeeded() throws IOException {
		if (indent) {
			indent = false;
			for (int i = 0; i < indents; i++) {
				appendable.append(' ').append(' ');
			}
		}
	}

	public Output put(Aware a) {
		try {
			indentIfNeeded();
			a.output(this);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void raw(char c) throws IOException {
		//indentIfNeeded();
		appendable.append(c);
	}

	public void raw(char[] buffer, int offset, int length) throws IOException {
		indentIfNeeded();
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
			indentIfNeeded();
			appendable.append(s);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(char c) {
		try {
			indentIfNeeded();
			appendable.append(c);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(Object o) {
		try {
			indentIfNeeded();
			raw(o);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void raw(@NullUnknown Object o) throws IOException {
    if (o == null) {
      appendable.append("<!null!>");
    } else if (o instanceof Aware aw) {
      aw.output(this);
    } else if (o instanceof CharSequence cs) {
      appendable.append(cs);
    } else if (o instanceof Character ch) {
      appendable.append(ch);
    } else if (o instanceof char[] ca) {
      for (char c : ca) appendable.append(c);
    } else {
      appendable.append(o.toString());
    }
	}

	public Output put(Object a0, Object a1) {
		try {
			indentIfNeeded();
			raw(a0);
			raw(a1);
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Output put(Object a0, Object a1, Object a2) {
		try {
			indentIfNeeded();
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
			indentIfNeeded();
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
			indentIfNeeded();
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

	public interface Aware {
		void output(Output o) throws IOException;
	}
}
