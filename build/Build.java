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
import static io.immutables.build.build.Sources.project;
import static io.immutables.build.build.Sources.scanSources;
import static io.immutables.build.build.Vendored.module;
import static io.immutables.build.build.Vendored.vendor;

interface Ver {
	String Immutables = "2.9.2";
	String Jsr305 = "3.0.1";
	String Jmh = "1.36";
	String Jackson = "2.14.2";
	String Guava = "31.1-jre";
	String Spring = "6.0.9";
}

interface Build {
	static void main(String... args) throws Exception {

		module("org.immutables.value", a -> a
			.classes("org.immutables:value", Ver.Immutables)
			.noSources()
		);
		module("org.immutables.value.annotations", a -> a
			.classes("org.immutables:value-annotations", Ver.Immutables)
		);
		module("javax.annotation.jsr305", a -> a
			.classes("com.google.code.findbugs:jsr305", Ver.Jsr305)
			.sources("com.google.code.findbugs:jsr305", Ver.Jsr305)
			.repackage()
		);
		module("org.junit.junit4", a -> a
			.classes("junit:junit", "4.12")
			.classes("org.hamcrest:hamcrest-core", "1.3")
			.repackage()
		);
		module("com.fasterxml.jackson.core", a -> a
			.classes("com.fasterxml.jackson.core:jackson-core", Ver.Jackson)
		);
		module("com.fasterxml.jackson.databind", a -> a
			.classes("com.fasterxml.jackson.core:jackson-databind", Ver.Jackson)
		);
		module("com.fasterxml.jackson.annotation", a -> a
			.classes("com.fasterxml.jackson.core:jackson-annotations", Ver.Jackson)
		);
		module("com.google.common", a -> a
			.classes("com.google.guava:guava", Ver.Guava)
		);
		module("spring.web", a -> a
			.classes("org.springframework:spring-web", Ver.Spring)
		);

		vendor();
		scanSources("src");
		project();
	}
}
