package io.immutables.declaration.processor;

import io.immutables.stencil.Generator;
import io.immutables.stencil.Template;
import io.immutables.stencil.template.ProcessingStencil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Generator
public abstract class SpringMvcs extends Template {
	final ProcessingStencil sources = new ProcessingStencil();

	abstract void generate(List<Declaration> declarations);

	static String pathOf(Declaration.Operation operation) {
		return frontSlashOff(operation.path().with(operation.fixedQuery()));
	}

	private static String frontSlashOff(String path) {
		return !path.isEmpty() && path.charAt(0) == '/' ? path.substring(1) : path;
	}

	protected static Iterable<String> doclines(String text) {
		return Stream.of(text.split("\\n"))
			.dropWhile(String::isBlank)
			.takeWhile(s -> !s.isBlank())
			::iterator;
	}

	protected static String mapp(Object o) {
		String string = o.toString();
		return string.charAt(0) + string.toLowerCase().substring(1);
	}
}
