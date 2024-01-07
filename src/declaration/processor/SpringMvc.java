package dev.declaration.processor;

import io.immutables.meta.Null;
import io.immutables.stencil.Generator;
import io.immutables.stencil.Template;
import io.immutables.stencil.template.ProcessingStencil;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.type.TypeMirror;

@Generator
abstract class SpringMvc extends TemplateBase {
	final ProcessingStencil sources = new ProcessingStencil();

	abstract void generate(Declaration.Module module);

	static String pathOf(Declaration.Operation operation) {
		var path = operation.path().with(operation.fixedQuery());
		return "${api.prefix}" + frontSlashOff(path);
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

	protected static String capitalize(Object o) {
		String s = o.toString();
		return s.charAt(0) + s.toLowerCase().substring(1);
	}

	static String stringify(Type type, TypeMirror javaType) {
		var string = javaType.toString();
		if (type instanceof Type.Container c
			&& c.container() == Type.Container.Kind.Nullable) {
			string = "@" + Null.class.getCanonicalName() + " " + string;
		}
		return string;
	}
}
