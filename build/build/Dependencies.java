package build;

import java.util.*;

record Dependency(String name, boolean isStatic, ProvidingModule module) {}

class Dependencies {
	static final Set<String> reservedJdkModules = Set.of(
		"java.base",
		"java.compiler");

	static final Map<String, ProvidingModule> definitions = new HashMap<>();
	static final Map<String, Map<String, Dependency>> dependencies = new HashMap<>();

	static ProvidingModule get(String name, ModuleInfo forModule) {
		var module = definitions.get(name);
		if (module != null) return module;

		throw new NoSuchElementException(
			"Dependency module '%s' cannot be found for '%s'".formatted(name, forModule.name()));
	}

	static Collection<Dependency> dependenciesOf(SourceModule module) {
		var name = module.moduleInfo().name();
		return dependenciesOf(name);
	}

	static Collection<Dependency> dependenciesOf(String name) {
		return dependencies.getOrDefault(name, Map.of()).values();
	}

	static void resolve() {
		// don't care about duplicates etc
		for (var v : Vendored.modules) definitions.put(v.name(), v);
		for (var s : Sources.modules) definitions.put(s.name(), s);

		// collect dependency sets including transitives
		for (var def : definitions.values()) {
			var ds = new HashMap<String, Dependency>();
			collectDependencies(def, false, ds);
			var duplicate = dependencies.put(def.name(), ds);
			assert duplicate == null;
		}
	}

	private static void collectDependencies(
		ProvidingModule thisModule, boolean forceStatic, Map<String, Dependency> ds) {

		if (thisModule instanceof SourceModule m) {
			for (var require : m.moduleInfo().requires()) {

				if (reservedJdkModules.contains(require.name())) {
					System.err.printf("Reserved dep: %s <- %s%n", require.name(), m.name());
					continue;
				}

				var otherModule = definitions.get(require.name());
				if (otherModule == null) {
					System.err.printf("Missing dep: %s <- %s%n", require.name(), m.name());
					continue;
				}
				// the dependency is considered static if it's either declared static
				// or is a transitive dependency of a static dependency
				boolean isStatic = forceStatic || require.isStatic();

				Dependency existing = ds.get(require.name());
				if (existing != null) {
					if (existing.isStatic() && !isStatic) {
						// The case where we meet the same dependency,
						// but this time it's non-static,
						// so we will reprocess the same dependency again,
						// but replacing with runtime now
						// (inverting the condition makes it unreadable
						assert true;
					} else continue;
				}

				ds.put(require.name(),
					new Dependency(require.name(), isStatic, otherModule));

				if (require.isTransitive()) collectDependencies(otherModule, isStatic, ds);
			}
		}
	}
}
