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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public interface Repackage {

	static Path mergeAutomaticModule(String module, List<Jar> jars) throws IOException {
		var vendored = Dirs.vendored(module);
		var merged = Files.createTempFile(
			Repackage.class.getSimpleName() + "_",
			vendored.getFileName().toString());

		try (var out = new ZipOutputStream(Files.newOutputStream(merged))) {
			var writtenEntries = new HashSet<>();

			for (var j : jars) {
				try (var in = new ZipFile(Dirs.downloaded(j).toFile())) {
					var entries = in.entries();
					while (entries.hasMoreElements()) {
						var next = entries.nextElement();
						var name = next.getName();

						if (writtenEntries.contains(name) || skippedEntry(name)) continue;

						writtenEntries.add(name);

						out.putNextEntry(new ZipEntry(name));
						// Any jar file must contain manifest as the first entry
						// Here we actually rely on that this entry will be the first
						// one in first jar
						if (name.equals(MANIFEST_ENTRY)) {
							var manifest = new Manifest(in.getInputStream(next));
							rewriteManifest(module, manifest).write(out);
							// We're effectively dropping all original attributes
						} else try (var ins = in.getInputStream(next)) {
							ins.transferTo(out);
						}
						out.closeEntry();
					}
				}
			}
		}

		Files.move(merged, vendored,
			StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

		return vendored;
	}

	private static Manifest rewriteManifest(String module, Manifest manifest) {
		var mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
		var newManifest = new Manifest();
		newManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		if (mainClass != null) {
			newManifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
		}
		newManifest.getMainAttributes().putValue(MANIFEST_AUTOMATIC_MODULE_NAME, module);
		// skipping any other entries
		return newManifest;
	}

	private static boolean skippedEntry(String name) {
		return name.endsWith("module-info.class") // always skip module-info when repackaging
			|| name.endsWith("/INDEX_LIST") // inhibitor fence
			|| name.endsWith("/INDEX.LIST") // inhibitor fence
			|| name.endsWith(".SF") // signatures
			|| name.endsWith(".RSA") // signatures
			|| name.endsWith(".DES") // signatures
			|| name.startsWith("META-INF/maven/"); // maven metadata
	}

	String MANIFEST_ENTRY = "META-INF/MANIFEST.MF";
	String MANIFEST_AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";
}
