package io.immutables.stencil.template;

import io.immutables.meta.Late;
import io.immutables.stencil.Current;
import io.immutables.stencil.Generator;
import io.immutables.stencil.Stencil;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

// Since we require records, it's appropriate be safe to require LTS release 17
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class Processor extends AbstractProcessor {
	private @Late ProcessingCurrent current;

	@Override public synchronized void init(ProcessingEnvironment processing) {
		super.init(processing);
		current = new ProcessingCurrent(processing);
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(Generator.class.getName());
	}

	@Override public Set<String> getSupportedOptions() {
		return Set.of(OPTION_IMMUTABLES_DIR);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
		if (round.processingOver()) {
			//
		} else try {
			var directory = processingEnv.getOptions().getOrDefault(OPTION_IMMUTABLES_DIR, "");
			var emitter = Current.use(current, Emitter::new);

			var annotatedGenerators = round.getElementsAnnotatedWith(Generator.class);
			for (var g : annotatedGenerators) {
				if (g.getKind() != ElementKind.CLASS) {
					error("Generator codebehind must be a class", g);
					continue;
				}

				var typeElement = (TypeElement) g;
				if (typeElement.getNestingKind() != NestingKind.TOP_LEVEL) {
					error("Generator codebehind must be a top level class", g);
					continue;
				}
				var packageName =
					((PackageElement) g.getEnclosingElement()).getQualifiedName().toString();
				var generatorName = g.getSimpleName().toString();
				var templateName = generatorName + ".generator";

				CharSequence content;
				if (!directory.isEmpty()) {
					content = Files.readString(Path.of(directory).resolve(templateName));
				} else try {
					content = processingEnv.getFiler()
						.getResource(StandardLocation.SOURCE_PATH, packageName, templateName)
						.getCharContent(false);
				} catch (IOException fromSourcePath) {
					// second attempt with no package name
					try {
						content = processingEnv.getFiler()
							.getResource(StandardLocation.SOURCE_PATH, "", templateName)
							.getCharContent(false);
					} catch (IOException fromSourcePathNoPackage) {
						var stackTrace = toStackTraceString(fromSourcePathNoPackage);//fromSourcePath);
						error("Cannot read %s file in the same package (via sourcepath): %s"
							.formatted(templateName, stackTrace), g);
						emitter.generateStub(packageName, generatorName, List.of(stackTrace));
						continue;
					}
				}

				var parser = new BrainDeadParser(content.toString().toCharArray());
				parser.tryParse(templateName);
				if (!parser.problems.isEmpty()) {
					// generating stub minimizes downstream file now found errors
					emitter.generateStub(packageName, generatorName, parser.problems);
					for (var p : parser.problems) {
						error(p.toString(), g);
					}
				} else {
					emitter.generate(packageName, generatorName,
						scopeOf(typeElement),
						parser.elements);
				}
			}

		} catch (Throwable e) {
			e.printStackTrace();
			error(e + "\n" + toStackTraceString(e));
		}
		// Regardless of what other processors might want, we claim our trigger.
		// it's very specialized, no need to overthink it
		return true;
	}

	private LocalScope scopeOf(TypeElement typeElement) {
		var allMembers = processingEnv.getElementUtils().getAllMembers(typeElement);

		var scope = new LocalScope(null, typeElement.getSimpleName().toString());

		for (var f : ElementFilter.fieldsIn(allMembers)) {
			if (f.getModifiers().contains(Modifier.PRIVATE)) continue;

			if (f.getSimpleName().toString().contains("_")) continue;

			if (((TypeElement) f.getEnclosingElement())
				.getQualifiedName().contentEquals(Stencil.class.getName())) continue;

			scope.declare(f.getSimpleName().toString());
		}
		return scope;
	}

	private void error(String message) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
	}

	private void error(String message, Element origin) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, origin);
	}

	private CharSequence toStackTraceString(Throwable e) {
		var w = new StringWriter();
		try (var pw = new PrintWriter(w)) {
			e.printStackTrace(pw);
		}
		return w.toString();
	}

	public static final String OPTION_IMMUTABLES_DIR = "io.immutables.dir";
}
