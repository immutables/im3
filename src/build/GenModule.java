package io.immutables.build;

import io.immutables.build.build.Dependencies;
import io.immutables.build.build.Dependency;
import io.immutables.build.build.SourceModule;
import java.util.Collection;
import java.util.List;

record GenModule(String shortName, SourceModule module) {
	String path() {
		return shortName.replace('.', '/');
	}

	String name() {
		return module.name();
	}

	List<GenModule> nested() {
		return Dependencies.nestedOf(module)
			.stream()
			.filter(SourceModule.class::isInstance)
			.map(SourceModule.class::cast)
			.map(m -> new GenModule(
				relativizeShortName(m.name()), m))
			.toList();
	}

	Collection<Dependency> dependencies() {
		return Dependencies.dependenciesOf(module);
	}

	private String relativizeShortName(String name) {
		return name.substring(module.name().length() + 1);
	}
}
