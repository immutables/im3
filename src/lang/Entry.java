package io.immutables.lang;

import io.immutables.lang.node.Term;
import io.immutables.lang.syntax.Tokenizer;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Entry {
	public static void main(String... args) throws IOException {
		var targetPackage = "index";
		var outputDirectory = ".";
		var watch = false;
		var sources = new ArrayList<Path>();

		for (int i = 0; i < args.length; i++) {
			var a = args[i];
			switch (a) {
			case "-w", "--watch" -> watch = true; // TODO
			case "-p", "--package" -> targetPackage = require(a, args, ++i);
			case "-d", "--output-dir" -> outputDirectory = require(a, args, ++i);
			default -> {
				if (isKeySwitch(a)) die("Unknown key ", a);
				sources.add(Path.of(a));
			}
			}
		}
		var currentDir = Path.of("").toAbsolutePath();
/* 	FIXME if (watch) {
			try (WatchService service = FileSystems.getDefault().newWatchService()) {
				currentDir.register(, StandardWatchEventKinds.ENTRY_MODIFY);
			}
		}*/
		if (!sources.isEmpty()) {
			for (var f : sources) {
				if (!Files.isReadable(f)) die("Not a readable file ", f);
			}
		} else {
			try (var stream = Files.walk(currentDir)) {
				stream.filter(f -> f.getFileName().toString().endsWith(SUFFIX))
					.filter(Files::isReadable)
					.forEach(sources::add);
			}
			if (sources.isEmpty()) {
				die("No readable sources provided or *", SUFFIX, " found in directory: ", currentDir);
			}
		}

		var p = new CompilePackage();
		p.name = validPackageName(targetPackage);
		p.output = Path.of(outputDirectory);
		p.sources = List.copyOf(sources);
		p.compile();
	}

	private static String validPackageName(String p) {
		var t = new Tokenizer(p.toCharArray());
		t.tokenize();
		t.terms.rewind();
		if (t.terms.count() != 1 || t.terms.next() != Term.Name) {
			die("Illegal package name: ", p);
		}
		return p;
	}

	private static boolean isKeySwitch(String a) {
		return a.startsWith("-") || a.startsWith("--");
	}

	private static String require(String key, String[] args, int index) {
		if (index >= args.length) die("Not enough arguments for ", key);
		var a = args[index];
		if (isKeySwitch(a)) {
			die("Missing argument for ", key, ". Encountered ", a, "instead");
		}
		return a;
	}

	private static void die(Object... message) {
		for (var m : message) System.err.print(m);
		System.err.println();
		System.exit(-128);
	}

	private static final String SUFFIX = ".rx";
}
