package io.immutables.stencil.test;

import io.immutables.stencil.FilesStencil;
import io.immutables.stencil.JavaStencil;
import io.immutables.stencil.Stencil;

public class Fun extends Stencil.Raw {
	final FilesStencil file = new FilesStencil();
	final JavaStencil java = new JavaStencil();

	void draw() {
		file.path("abc.txt", () -> {
			var o = out();
			o.indents++;

			java.comments("""
				Every
				Single
				Line
				""");

			o.indents--;

			java.doc()
				.content("Block block", "Comment comment");
		});

		/*for (var i = 0; i < 3; i++) {
			file.path("my/_", i, ".i")
				.content("""
					Just couple of lines
					Here or there #%s
					""".formatted(i));
		}*/
	}
}
