package io.immutables.declaration.processor;

import io.immutables.declaration.ServiceInventory;
import io.immutables.stencil.Current;
import io.immutables.stencil.template.ProcessingCurrent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

// Since we require records, it should be safe to require LTS release 17
@SupportedSourceVersion(SourceVersion.RELEASE_17)
/*@SupportedOptions({
	Processor.OPTION_X
})*/
public class Processor extends AbstractProcessor {
//	static final String OPTION_X = "io.immutables.declaration.x";

	private InventoryDiscoverer discoverer;
	private ProcessingCurrent current;

	@Override public synchronized void init(ProcessingEnvironment processing) {
		super.init(processing);
		discoverer = new InventoryDiscoverer(processing);
		current = new ProcessingCurrent(processing);
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(ServiceInventory.class.getName());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
		if (round.processingOver()) {
			//
		} else {
			try {
				var inventoryPackages = round.getElementsAnnotatedWith(ServiceInventory.class);
				var endpoints = Current.use(current, SpringMvcInterfaces::new);
				var endpoints2 = Current.use(current, SpringMvcs_generator::new);

				for (var p : inventoryPackages) {
					var declarations = discoverer.discover(p);

					endpoints.generate(declarations);
					endpoints2.generate(declarations);
				}
			} catch (Throwable e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
					e + "\n" + toStackTraceString(e));
			}
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
