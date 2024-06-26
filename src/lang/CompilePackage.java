package io.immutables.lang;

import io.immutables.lang.back.Js;
import io.immutables.lang.back.Output;
import io.immutables.lang.back.PrintTree;
import io.immutables.lang.back.JsTranslator;
import io.immutables.lang.node.Node;
import io.immutables.lang.syntax.Lang;
import io.immutables.lang.syntax.ForLang;
import io.immutables.lang.syntax.Unit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CompilePackage {
	String name;
	Path output;
	List<Path> sources;

	List<Unit<Node.Unit>> units = new ArrayList<>();

	Path resultFile;
	ForLang context;

	void compile() throws IOException {
		createUnits();
		if (!parseUnits()) System.exit(1);

		constructUnits();
		writeUnits();
	}

	private void writeUnits() throws IOException  {
		resultFile = Files.createDirectories(output).resolve(name + ".js");

		var printer = new PrintTree(new Output(System.err));
		for (var u : units) {
			System.err.println(u.path() + ":" + u.message());
			printer.print(u.node());
		}

		context.identifiers.markBackendKeywords(Js.KEYWORDS);

		try (var w = Files.newBufferedWriter(resultFile,
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING)) {

			var appendable = new StringBuilder();

			var translator = new JsTranslator(name, new Output(appendable));
			translator.units(units);
			String s = appendable.toString();
			System.out.println(s);
			w.append(s);
		} // handle catch later
	}

	private void constructUnits() {
		context = new ForLang();
		for (var u : units) {
			u.construct(context);
		}
	}

	private boolean parseUnits() {
		boolean ok = true;
		for (var u : units) {
			if (!u.parse()) {
			//	u.printCodes();
			//	u.printTerms();
			//	u.printProductions();

				ok = false;
				try {
					System.err.println(u.path() + ":" + u.message());
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			} else {
			//	u.printProductions();
			}
		}
		return  ok;
	}

	private void createUnits() {
		for (var path : sources) {
			try {
				// Would be better to read directly into char[],
				// or read UTF-8 byte[] and parse it as such
				var input = Files.readString(path).toCharArray();
				units.add(new Unit<>(path, input, Lang.VM, Lang.Unit));
			} catch (IOException e) {
				System.err.println(path + ": " + e.getMessage());
				System.exit(1);
			}
		}
	}
}
