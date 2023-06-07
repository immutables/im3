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

import java.util.List;

public record ModuleInfo(
	String name,
	boolean open,
	List<Require> requires,
	List<Processor> processors,
	List<String> options) {

	public record Require(String name, boolean isStatic, boolean isTransitive) {}

	/**
	 * {@link Processor} This is not a real part of standard module-info syntax, it's doclet-like
	 * special comment directive
	 */
	public record Processor(String name) {}
}
