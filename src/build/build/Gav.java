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

import java.net.URI;

public record Gav(
	String group,
	String artifact,
	String version,
	String classifier) {

	public Gav(String group, String artifact, String version) {
		this(group, artifact, version, "");
	}

	public Gav {
		assert !group.isEmpty();
		assert !artifact.isEmpty();
		assert !version.isEmpty();
	}

	URI toMvnUri(String prefix, String suffix) {
		var dirs = "%s/%s/%s/".formatted(group.replace('.', '/'), artifact, version);
		return URI.create(prefix + dirs + toFilenameBase() + suffix);
	}

	String toFilenameBase() {
		return !classifier.isEmpty()
			? "%s-%s-%s".formatted(artifact, classifier, version)
			: "%s-%s".formatted(artifact, version);
	}

	@Override public String toString() {
		return !classifier.isEmpty()
			? "%s:%s:%s:%s".formatted(group, artifact, classifier, version)
			: "%s:%s:%s".formatted(group, artifact, version);
	}
}
