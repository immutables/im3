package io.immutables.declaration.processor;

import io.immutables.stencil.JavaStencil;
import io.immutables.stencil.Stencil;
import io.immutables.stencil.template.ProcessingStencil;
import java.util.List;
import static java.util.stream.Collectors.joining;

class SpringMvcInterfaces extends Stencil.Raw {
	final ProcessingStencil processing = new ProcessingStencil();
	final JavaStencil java = new JavaStencil();

	public void generate(List<Declaration> declarations) {

		for (var d : declarations) {
			if (d instanceof Declaration.Contract contract) {
				processing.inPackage(contract.module(), ".mvc")
					.name(contract.name())
					.java(() -> generateContract(contract));
			}
		}
	}

	void generateContract(Declaration.Contract contract) {
		put("package ", contract.module(), ".mvc;").ln().ln();
		put("import org.springframework.web.bind.annotation.*;").ln().ln();

		put("@io.immutables.meta.NonnullByDefault").ln();
		put("@RestController").ln();
		put("public interface ", contract.name(),
			" extends ", contract.module(), ".", contract.name(), " ");
		java.braces(() -> {
			for (var operation : contract.operations().values()) {
				ln();

				generateOperation(operation);
			}
		});
	}

	private void generateOperation(Declaration.Operation operation) {
		java.doc(() -> put("Operation ", operation.name()));

		annotations(operation);

		put(operation.returns().type(), " ", operation.name());

		parameterList(operation);
		throwsClause(operation);

		put(";").ln();
	}

	private void annotations(Declaration.Operation operation) {
		put("@java.lang.Override").ln();
		put("@ResponseBody").ln();
		put("@RequestMapping(path = ", java.literal(pathOf(operation)),
			", method = RequestMethod.", operation.method(), ")").ln();
	}

	private void throwsClause(Declaration.Operation operation) {
		if (!operation.thrown().isEmpty()) {
			out().indents += 2;
			ln().put("throws ");
			out().indents -= 2;
			put(operation.thrown().stream()
				.map(Declaration.Thrown::type)
				.map(Object::toString)
				.collect(joining(", ")));
		}
	}

	private void parameterList(Declaration.Operation operation) {
		put("(");
		// TODO - arglist stencil
		out().indents += 2;
		for (var p : operation.parameters()) {
			if (p.index() == 0) out().ln();
			else put(",").ln();

			switch (p.mapping()) {
				case Path -> out().put("@PathVariable(", java.literal(p.name()), ") ");
				case Query -> out().put("@RequestParam(", java.literal(p.name()), ") ");
				case Body -> out().put("@RequestBody ");
				case Unmapped -> {/*Error case, we leave it without annotation*/}
			}

			out().put(p.type(), " ", p.name());
		}
		out().indents -= 2;
		put(")");
	}

	private static String pathOf(Declaration.Operation operation) {
		return frontSlashOff(operation.path().with(operation.fixedQuery()));
	}

	private static String frontSlashOff(String path) {
		return !path.isEmpty() && path.charAt(0) == '/' ? path.substring(1) : path;
	}
}
