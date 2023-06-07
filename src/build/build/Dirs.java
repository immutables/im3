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

import java.nio.file.Path;

public interface Dirs {
	Path project = Path.of(".");

	Path lib = Path.of("lib");
	Path build = Path.of(".build");
	Path artifacts = build.resolve("artifacts");
	Path modules = lib.resolve("modules");

	Path idea = Path.of(".idea");
	Path ideaLibraries = idea.resolve("libraries");
	Path ideaModules = idea.resolve("modules");
	Path generatedContent = build.resolve("generated");
	Path generatedTestContent = build.resolve("generated");

	static Path downloaded(Jar jar) {
		return artifacts.resolve(jar.gav().group()).resolve(jar.toFilename());
	}

	static Path vendored(String module) {
		return modules.resolve(module + Mvn.ext_jar);
	}
}
