// Copyright 2023 Immutables Authors and Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package io.immutables.build.build;

import java.util.*;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toUnmodifiableSet;

public class Dependencies {
	static final Set<String> reservedJdkModules = Set.of(
		"java.base",
		"java.sql",
		"java.compiler",
		"java.net.http");

	static final Map<String, ProvidingModule> definitions = new HashMap<>();
	static final Map<String, Map<String, Dependency>> dependencies = new HashMap<>();

	public static ProvidingModule get(String name) {
		var module = definitions.get(name);
		if (module != null) return module;
		throw new NoSuchElementException("No module named '" + name + "'");
	}

	public static ProvidingModule get(String name, ModuleInfo forModule) {
		var module = definitions.get(name);
		if (module != null) return module;

		throw new NoSuchElementException(
			"Dependency module '%s' cannot be found for '%s'".formatted(name, forModule.name()));
	}

	public static Set<ProvidingModule> nestedOf(String name) {
		var prefix = name + ".";
		var module = get(name);
		if (module instanceof SourceModule s) return definitions.values()
				.stream()
				.filter(m -> m.name().startsWith(prefix))
				.collect(toUnmodifiableSet());
		return Set.of();
		/* FIXME
		return switch (get(name)) {
			case SourceModule s -> definitions.values()
				.stream()
				.filter(m -> m.name().startsWith(prefix))
				.collect(toUnmodifiableSet());
			default -> Set.of();
		};*/
	}

	public static Collection<ProvidingModule> nestedOf(SourceModule module) {
		return nestedOf(module.name());
	}

	public static Collection<Dependency> dependenciesOf(SourceModule module) {
		var name = module.moduleInfo().name();
		return dependenciesOf(name);
	}

	public static Collection<Dependency> dependenciesOf(String name) {
		return dependencies.getOrDefault(name, Map.of()).values();
	}

	public static void resolve() {
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
