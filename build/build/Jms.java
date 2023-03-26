package build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ModuleInfo(
	String name,
	boolean open,
	List<Require> requires,
	List<Processor> processors) {

	record Require(String name, boolean isStatic, boolean isTransitive) {}

	/**
	 * {@link Processor} This is not a real part of standard module-info syntax, it's doclet-like
	 * special comment directive
	 */
	record Processor(String name) {}
}

class MalformedModuleException extends Exception {
	final String problem;
	final Path file;

	MalformedModuleException(String problem, Path file) {
		this.problem = problem;
		this.file = file;
	}

	@Override public String getMessage() {
		return file + ": " + problem;
	}
}

public interface Jms {
	String MODULE_INFO_JAVA = "module-info.java";
	// These patterns require reasonable formatting, not a precise parsing
	Pattern LineComment = Pattern.compile("^\\s*//.*$");

	Pattern Module = Pattern.compile("^\\s*"
		+ "(?<open>open\\s+)?module\\s+"
		+ "(?<module>[a-zA-Z0-9_.]+)\\s*(\\{\\s*)?$");

	Pattern Requires = Pattern.compile("^\\s*requires\\s+"
		+ "(?<static>static\\s+)?"
		+ "(?<transitive>transitive\\s+)?"
		+ "(?<module>[a-zA-Z0-9._]+)\\s*;\\s*$");

	Pattern Processor = Pattern.compile("^\\s*"
		+ "(?://\\s*)?"
		+ "(?:[*]\\s*)?"
		+ "@processor\\s+"
		+ "(?<module>[a-zA-Z0-9._]+)\\s*");

	static ModuleInfo parseModuleInfo(Path file) throws IOException, MalformedModuleException {
		boolean open = false;
		String module = "";
		var requires = new ArrayList<ModuleInfo.Require>();
		var processors = new ArrayList<ModuleInfo.Processor>();

		for (var line : Files.readAllLines(file)) {
			Matcher m;

			m = Processor.matcher(line);
			if (m.matches()) {
				var processorModule = m.group("module");

				processors.add(new ModuleInfo.Processor(processorModule));
				continue;
			}

			m = Module.matcher(line);
			if (m.matches()) {
				open = m.group("open") != null;
				module = m.group("module");
				continue;
			}

			m = Requires.matcher(line);
			if (m.matches()) {
				var requiredModule = m.group("module");
				var isStatic = m.group("static") != null;
				var isTransitive = m.group("transitive") != null;

				requires.add(new ModuleInfo.Require(requiredModule, isStatic, isTransitive));
				continue;
			}

			// no need for now
			//if (LineComment.matcher(line).matches()) continue;
		}

		if (module.isEmpty()) throw new MalformedModuleException("No module name", file);

		return new ModuleInfo(module, open, List.copyOf(requires), List.copyOf(processors));
	}
/*
	static void main(String[] args) throws IOException, MalformedModuleException {
		var file = "/Users/Shared/Git/immutaverse/src/common/_test/module-info.java";
		System.out.println(Jms.parseModuleInfo(Path.of(file)));
	}*/
}
