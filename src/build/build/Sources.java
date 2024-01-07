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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Sources {
	private Sources() {}

	public static final List<SourceModule> modules = new ArrayList<>();

	public static void scanSources(String start) throws IOException {
		var searchPath = Path.of(start);
		Files.walkFileTree(searchPath, Set.of(), MAX_DEPTH,
			new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (attrs.isRegularFile()
						&& file.getFileName().toString().equals(Jms.MODULE_INFO_JAVA)) {

						try {
							readModule(file, searchPath);
						} catch (IOException | MalformedModuleException e) {
							// TODO
							e.printStackTrace();
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
	}

	public static void project() throws IOException {
		Dependencies.resolve();
		Idea.libraries();
		Idea.modules();
		Idea.compiler();
	}

	private static void readModule(Path file, Path searchPath)
		throws IOException, MalformedModuleException {

		var dir = file.getParent();

		var projectDir = Path.of(".").toAbsolutePath().normalize();
		var moduleDir = dir.toAbsolutePath();
		var moduleInfo = Jms.parseModuleInfo(file, moduleDir, projectDir);

		modules.add(new SourceModule(dir, searchPath.relativize(dir), moduleInfo));
	}

	private static final int MAX_DEPTH = 30;
}
