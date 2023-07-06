package io.immutables.declaration.processor;

import io.immutables.declaration.ServiceInventory;
import io.immutables.meta.Late;
import io.immutables.stencil.template.ProcessingCurrent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public abstract class DeclarationsProcessor extends AbstractProcessor {
	private @Late InventoryDiscoverer discoverer;
	private @Late ProcessingCurrent current;

	@Override
	public synchronized void init(ProcessingEnvironment processing) {
		super.init(processing);
		discoverer = new InventoryDiscoverer(processing);
		current = new ProcessingCurrent(processing);
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

	/**
	 * Process (
	 * @param packageName
	 * @param declarations
	 */
	protected abstract void process(String packageName, Collection<Declaration> declarations);

	/**
	 * Here we can finalize and generate any summary including all processed declarations.
	 * @param declarations collection of all declarations visited
	 */
	protected void over(Collection<Declaration> declarations) {}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
		if (round.processingOver()) {
			over(discoverer.allDeclarations());
			return true;
		}

		try {
			var inventoryPackages = round.getElementsAnnotatedWith(ServiceInventory.class);
			for (var p : inventoryPackages) {
				var packageName = ((PackageElement) p).getQualifiedName().toString();
				var declarations = discoverer.discover(p);
				process(packageName, declarations);
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
