package io.immutables.stencil;

import io.immutables.meta.Null;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilesStencil extends Stencil.Privileged {
	public void path(String path, Runnable content) {
		var dir = (Directory) current();
		Path file = dir.path.resolve(path);
		render(file, content);
	}

	public interface Content {
		void content(Runnable content);
		void content(CharSequence content);
		void content(String... lines);
		void content(Iterable<? extends CharSequence> lines);

		default void content(Stream<? extends CharSequence> lines) {
			content(lines.collect(Collectors.toList()));
		}
	}

	public Content path(Object... path) {
		var dir = (Directory) current();
		Path file = dir.path.resolve(stringify(path));

		return new Content() {
			public void content(Runnable content) {
				render(file, content);
			}

			public void content(String... lines) {
				try {
					Files.createDirectories(file.getParent());
					Files.write(file, Arrays.asList(lines),
						StandardCharsets.UTF_8, StandardOpenOption.CREATE);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			public void content(CharSequence content) {
				try {
					Files.createDirectories(file.getParent());
					Files.writeString(file, content,
						StandardCharsets.UTF_8, StandardOpenOption.CREATE);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			public void content(Iterable<? extends CharSequence> lines) {
				try {
					Files.createDirectories(file.getParent());
					Files.write(file, lines,
						StandardCharsets.UTF_8,
						StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
	}

	private void render(Path file, Runnable content) {
		var inner = new Output();
		@Null Output outer = reset(inner);
		try {
			content.run();
		} finally {
			reset(outer);
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, inner.toString(),
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String stringify(Object[] path) {
		var out = new Output();
		out.putAll(path);
		return out.toString();
	}
}
