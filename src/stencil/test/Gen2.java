package io.immutables.stencil.test;

import io.immutables.stencil.*;
import java.nio.file.Path;

@Generator
public abstract class Gen2 extends Template {
	final FilesStencil files = new FilesStencil();

	abstract void dohere();

	public static void main(String[] args) {
		Gen2 g = Current.use(new Directory(Path.of(".")), Gen2_generator::new);
		g.dohere();
	}

	interface Cc {}
	record Aa() implements Cc {}
	record Bb() implements Cc {}
}
