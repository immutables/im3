package io.immutables.stencil.test;

import io.immutables.stencil.BufferedCurrent;
import io.immutables.stencil.Current;
import io.immutables.stencil.Directory;
import java.nio.file.Path;
import org.junit.Test;

public class TestStencil {

	@Test public void test() {

	}

	public static void main(String[] args) {
/*		Directory directory = new Directory(Path.of("."));

		Fun fun = Current.use(directory, Fun::new);
		fun.draw();*/
		var c = new BufferedCurrent();
		var gen = Current.use(c, Gen_generator::new);
		gen.generate();

		System.out.println(c.content());
	}
}
