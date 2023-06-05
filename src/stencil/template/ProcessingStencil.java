package io.immutables.stencil.template;

import io.immutables.meta.Null;
import io.immutables.stencil.Output;
import io.immutables.stencil.Stencil;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.annotation.processing.Filer;

public class ProcessingStencil extends Stencil.Privileged {
	public InPackage inPackage(Object... packageName) {
		return inPackage(stringify(packageName));
	}

	public InPackage inPackage(String packageName) {
		return new InPackage() {
			public void name(String name, InJava inJava) {
				name(name).java(inJava);
			}

			public JavaContent name(Object... name) {
				return name(stringify(name));
			}

			JavaContent name(String name) {
				return new JavaContent() {
					public void java(InJava inJava) {
						render(packageName, name, () -> inJava.run(packageName, name));
					}

					public void java(Runnable runnable) {
						render(packageName, name, runnable);
					}
				};
			}
		};
	}

	public interface InPackage {
		void name(String name, InJava inJava);
		JavaContent name(Object... name);
	}

	public interface JavaContent {
		void java(Runnable runnable);
		void java(InJava inJava);
	}

	public interface InJava {
		void run(String packageName, String className);
	}

	private void render(String inPackage, String name, Runnable content) {
		var inner = new Output();
		inner.indentStep = "  ";
		@Null var outer = reset(inner);
		try {
			content.run();
		} finally {
			reset(outer);
		}
		var current = (ProcessingCurrent) current();
		try {
			var filer = current.processing.getFiler();
			var file = filer.createSourceFile(inPackage + "." + name);
			try (var w = file.openWriter()) {
				w.write(inner.toString());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String stringify(Object[] path) {
		var out = new Output();
		out.putAll(path);
		return out.toString();
	}
}
