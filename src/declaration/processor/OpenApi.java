package dev.declaration.processor;

import io.immutables.stencil.FilesStencil;
import io.immutables.stencil.Generator;
import io.immutables.stencil.Literals;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.groupingBy;

@Generator
abstract class OpenApi extends TemplateBase {
	final FilesStencil files = new FilesStencil();

	abstract void generate(Module module);

	Map<String, List<Declaration.Operation>> groupByPath(
		Collection<Declaration.Operation> operations) {
		return operations.stream().collect(groupingBy(
			op -> op.path().path));
	}

	List<Declaration.Parameter> nonBodyParameters(Declaration.Operation operation) {
		return operation.parameters().stream()
			.filter(p -> switch (p.mapping()) {
				case Path, Query -> true;
				default -> false;
			})
			.toList();
	}

	// this should be safer to downstream OpenApi use
	String typeName(Reference reference) {
		return reference.name().replace('.', '_');
	}

	CharSequence maybeLiteral(String identifier) {
		return SAFE_IDENTIFIER.matcher(identifier).matches()
			? identifier
			: Literals.string(identifier);
	}

	private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
		"^[a-zA-Z][.a-zA-Z0-9_-]*$");
}
