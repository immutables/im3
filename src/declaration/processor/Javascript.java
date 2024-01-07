package dev.declaration.processor;

import io.immutables.stencil.FilesStencil;
import io.immutables.stencil.Generator;
import io.immutables.stencil.Literals;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Generator
abstract class Javascript extends TemplateBase {
	final FilesStencil files = new FilesStencil();

	abstract void generate(Declaration.Module module);

	Map<String, List<Declaration.Datatype>>
	datatypesByNamespace(List<Declaration> declarations) {
		// Sorted tree map will give use shorter namespaces before longer ones
		// this is important for template
		return new TreeMap<>(declarations.stream().<Declaration.Datatype>mapMulti((declaration, cons) -> {
			if (declaration instanceof Declaration.Datatype dt) {
				cons.accept(dt);
			}
		}).collect(Collectors.groupingBy(dt -> namespaceOf(dt.name()))));
	}

	CharSequence maybeLiteral(String identifier) {
		return SAFE_IDENTIFIER.matcher(identifier).matches()
			? identifier
			: Literals.string(identifier);
	}

	enum ReturnContent {
		Json,
		Text,
		Blob,
		Void
	}

	ReturnContent returns(Declaration.Operation operation) {
		var type = operation.returns().type();
		if (type instanceof Type.Primitive primitive) {
			return switch (primitive) {
				case Void, Null ->  ReturnContent.Void;
				default -> ReturnContent.Text;
			};
		}
		if (type instanceof Type.Extended extended) {
			return extended == Type.Extended.Bytes ? ReturnContent.Blob : ReturnContent.Text;
		}
		return ReturnContent.Json;
	}

	enum SendContent {
		Text,
		Blob,
		Json,
		Form,
		Void
	}

	String templatePath(Declaration.Operation operation) {
		return "`" + operation.path().path.replace("{", "${") + "`";
	}

	SendContent sends(Declaration.Operation operation) {
		for (var p : operation.parameters()) {
			if (p.mapping() == Declaration.Parameter.Mapping.Body) {
				return sends(p.type());
			}
		}
		if (operation.method() == Declaration.HttpMethod.POST) {
			return SendContent.Form;
		}
		return SendContent.Void;
	}

	private SendContent sends(Type type) {
		if (type instanceof Type.Primitive p) {
			return switch (p) {
				case Null, Void -> SendContent.Void;
				default -> SendContent.Text;
			};
		}
		if (type instanceof Type.Extended e) {
			return e == Type.Extended.Bytes
				? SendContent.Blob
				: SendContent.Text;
		}
		return SendContent.Json;
	}

	private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
		"^[a-zA-Z][a-zA-Z0-9_]*$");
}
