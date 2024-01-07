package dev.declaration.processor;

import dev.declaration.ServiceInventory;
import io.immutables.meta.Late;
import io.immutables.meta.Null;
import io.immutables.stencil.Current;
import io.immutables.stencil.Directory;
import io.immutables.stencil.template.ProcessingCurrent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class Processor extends AbstractProcessor {
	private static final String OPTION_API_DIR = "dev.declaration.processor.output";
	private static final String OPTION_SERVERS = "dev.declaration.servers";

	private final Map<String, List<Declaration>> declarationsByPackage = new HashMap<>();

	private @Late InventoryDiscoverer discoverer;
	private @Late ProcessingCurrent current;

	private @Late SpringMvc mvcs;
	private @Null OpenApi apis;
	private @Null Javascript js;
	private @Null Markdown md;

	@Override
	public synchronized void init(ProcessingEnvironment processing) {
		super.init(processing);

		discoverer = new InventoryDiscoverer(processing);
		current = new ProcessingCurrent(processing);

		mvcs = Current.use(current(), SpringMvc_generator::new);

		var apiDir = processing.getOptions().getOrDefault(OPTION_API_DIR, "");
		if (!apiDir.isEmpty()) {
			var directory = new Directory(Path.of(apiDir));

			apis = Current.use(directory, OpenApi_generator::new);
			js = Current.use(directory, Javascript_generator::new);
			md = Current.use(directory, Markdown_generator::new);
		}

		var serversOption = processing.getOptions().getOrDefault(OPTION_SERVERS, "");
		if (!serversOption.isEmpty()) {
			var servers = new ArrayList<TemplateBase.Server>();

			for (var s : serversOption.split(";")) {
				int index = s.indexOf(">");
				if (index < 0) continue;
				var env = s.substring(0, index);
				var uri = s.substring(index + 1);
				servers.add(new TemplateBase.Server(env, uri));
			}

			if (apis != null) apis.servers().addAll(servers);
			if (js != null) js.servers().addAll(servers);
			if (md != null) md.servers().addAll(servers);
		}
	}

	protected final ProcessingCurrent current() {
		return current;
	}

	/** Since we require records, it should be safe to require LTS release 17. */
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_17;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(ServiceInventory.class.getName());
	}

	@Override
	public Set<String> getSupportedOptions() {
		return Set.of(OPTION_API_DIR, OPTION_SERVERS);
	}

	/**
	 * Process declaration package, generate some artifacts etc.
	 */
	private void process(Declaration.Module module) {
		mvcs.generate(module);

		if (apis != null) apis.generate(module);
		if (js != null) js.generate(module);
		if (md != null) md.generate(module);
	}

	/**
	 * Here we can finalize and generate any summary including all processed declarations.
	 * @param declarations collection of all declarations visited
	 */
	private void over(Map<String, List<Declaration>> declarations) {}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
		if (round.processingOver()) {
			over(declarationsByPackage);
			return true;
		}

		try {
			var inventoryPackages = round.getElementsAnnotatedWith(ServiceInventory.class);
			for (var p : inventoryPackages) {
				var module = discoverer.discover((PackageElement) p);
				declarationsByPackage.put(module.name(), module.declarations());
				process(module);
			}
		} catch (Throwable e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
				e + "\n" + toStackTraceString(e));
		}
		// Regardless of what other processors might want, we claim our trigger
		// annotation which we put on a package level.
		return true;
	}

	private CharSequence toStackTraceString(Throwable e) {
		var w = new StringWriter();
		try (var pw = new PrintWriter(w)) {
			e.printStackTrace(pw);
		}
		return w.toString();
	}
}
