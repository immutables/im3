package dev.declaration.processor;

import io.immutables.stencil.FilesStencil;
import io.immutables.stencil.Generator;
import java.util.*;

@Generator
abstract class Markdown extends TemplateBase {
	final FilesStencil files = new FilesStencil();

	abstract void generate(Module module);

	String linkTo(Declaration from, Module module) {
		return (module.name().equals(from.module())
			? "."
			: ("../" + module.name())) + "/readme.md";
	}

	String linkTo(Reference from, Reference to) {
		return ((to.module().equals(from.module())
			? "."
			: ("../" + to.name())) + "/" + filenameAndHashTo(to.name()));
	}

	String linkTo(Declaration.Contract contract) {
		return "./" + contract.name() + ".md";
	}

	String typenameRef(Reference from, Reference of) {
		return (!of.module().equals(from.module()) ? of.module() + "." : "")
			+ of.name();
	}

	private String filenameAndHashTo(String name) {
		return nsSegmentsOf(name).get(0) + ".md#" + name;
	}

	String linkTo(Declaration.Datatype datatype) {
		return "./" + filenameAndHashTo(datatype.name());
	}

	Map<String, Set<Declaration.Datatype>> datatypesByTopLevel(
		List<Declaration> declarations) {
		var byTopLevel = new HashMap<String, Set<Declaration.Datatype>>();

		for (var d : declarations) {
			if (d instanceof Declaration.Datatype datatype) {
				var top = toplevelOf(datatype.name());
				var key = !top.isEmpty() ? top : datatype.name();

				byTopLevel.computeIfAbsent(key,
						k -> new TreeSet<>(Comparator.comparing(Declaration::name)))
					.add(datatype);
			}
		}

		return byTopLevel;
	}
}
