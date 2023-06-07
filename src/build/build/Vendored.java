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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Vendored {
	private Vendored() {}

	public interface Artifacts {
		Artifacts classes(String groupArtifactClassifier, String version);
		Artifacts sources(String groupArtifactClassifier, String version);
		Artifacts noSources();
		Artifacts repackage();
	}

	static final List<VendorModule> modules = new ArrayList<>();

	public static void module(String name, Consumer<Artifacts> collectArtifacts) {
		var classJars = new ArrayList<Jar>();
		var sourceJars = new ArrayList<Jar>();
		var handle = new Artifacts() {
			boolean repackage;
			boolean noSources;

			@Override public Artifacts classes(String gac, String v) {
				classJars.add(new Jar(gav(gac, v), Jar.Kind.Classes));
				return this;
			}

			@Override public Artifacts sources(String gac, String v) {
				sourceJars.add(new Jar(gav(gac, v), Jar.Kind.Sources));
				return this;
			}

			@Override public Artifacts noSources() {
				this.noSources = true;
				return this;
			}

			@Override public Artifacts repackage() {
				this.repackage = true;
				return this;
			}
		};
		collectArtifacts.accept(handle);

		assert !classJars.isEmpty()
			: "At least one classes jar have to be specified for module `%s`".formatted(name);
		assert handle.repackage || classJars.size() == 1
			: "If many class artifacts are specified, module `%s` have to be repackages".formatted(name);
		assert !handle.noSources || sourceJars.isEmpty()
			: "If noSources(), then sources() should not be included";

		if (sourceJars.isEmpty() && !handle.noSources) {
			for (var j : classJars) {
				sourceJars.add(new Jar(j.gav(), Jar.Kind.Sources));
			}
		}

		modules.add(new VendorModule(
			name,
			List.copyOf(classJars),
			List.copyOf(sourceJars),
			handle.repackage));
	}

	private static Gav gav(String gac, String v) {
		var parts = gac.split(":");
		return switch (parts.length) {
			case 2 -> new Gav(parts[0], parts[1], v);
			case 3 -> new Gav(parts[0], parts[1], v, parts[2]);
			default -> throw new AssertionError(
				"wrong format '%s' =/=> group:artifact[:classifier]".formatted(gac));
		};
	}

	public static void vendor() throws IOException, InterruptedException {
		ensureAllDownloaded();
		ensureVendored();
	}

	private static void ensureVendored() throws IOException {
		Files.createDirectories(Dirs.modules);

		for (var m : modules) {
			var jars = m.classJars();
			if (m.repackage()) {
				Repackage.mergeAutomaticModule(m.name(), jars);
			} else {
				assert jars.size() == 1;
				Files.copy(
					Dirs.downloaded(jars.get(0)),
					Dirs.vendored(m.name()),
					StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private static void ensureAllDownloaded() throws IOException, InterruptedException {
		for (var m : modules) {
			for (var j : m.classJars()) ensureDownloaded(j);
			for (var j : m.sourceJars()) ensureDownloaded(j);
		}
	}

	private static void ensureDownloaded(Jar jar) throws IOException, InterruptedException {
		var path = Dirs.downloaded(jar);

		if (Files.exists(path)) return;

		Files.createDirectories(path.getParent());

		var uri = jar.toMvnUri();

		Http.fetch(uri, path);

		System.out.printf("Fetched %s (%.1fkb)%n", uri, Files.size(path) / 1024f);
	}
}
