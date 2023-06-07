package io.immutables.stencil;

import io.immutables.meta.Null;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilesStencil extends Stencil.Privileged {

	public void path(String path, Runnable content) {
		var dir = (Directory) current();
		Path file = dir.path.resolve(path);
		render(file, content);
	}

	public Path root() {
		var dir = (Directory) current();
		return dir.path;
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
		Path file = dir.path.resolve(concat(path));

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

	private static String concat(Object[] path) {
		var out = new Output();
		out.putAll(path);
		return out.toString();
	}


	public WithFiles dir(Path path) {
		var current = (Directory) current();
		return new WithFiles(current.path, path);
	}

	public WithFiles dir(Object... fromDir) {
		var current = (Directory) current();
		var from = current.path;
		if (fromDir.length > 0) {
			from = from.resolve(concat(fromDir));
		}
		return new WithFiles(current.path, from);
	}

	public static class WithFiles {
		private final Path root;
		private final Path dir;

		private final List<Glob> includes = new ArrayList<>();
		private final List<Glob> excludes = new ArrayList<>();

		WithFiles(Path root, Path dir) {
			this.root = root;
			this.dir = dir;
		}

		public <T> WithFiles forEach(
			Iterable<T> elements,
			BiConsumer<WithFiles, T> consumer) {

			for (var e : elements) {
				consumer.accept(this, e);
			}
			return this;
		}

		public WithFiles include(Object... include) {
			includes.add(Glob.of(concat(include)));
			return this;
		}

		public WithFiles exclude(Object... excludeParts) {
			excludes.add(Glob.of(concat(excludeParts)));
			return this;
		}

		public void copyTo(Object... targetParts) {
			var targetDir = root.resolve(concat(targetParts));
			if (!Files.exists(dir)) return;
			try {
				try (var stream = Files.walk(dir)) {
					var matching = stream.filter(path ->
						matches(dir.relativize(path))).toList();

					for (var source : matching) {
						var relative = dir.relativize(source);
						var targetFile = targetDir.resolve(relative);
						Files.createDirectories(targetFile.getParent());
						Files.copy(source, targetFile,
							StandardCopyOption.REPLACE_EXISTING);
					}
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private boolean matches(Path path) {
			return (includes.isEmpty()
				|| includes.stream().anyMatch(p -> p.matches(path.toString())))
				&& excludes.stream().noneMatch(p -> p.matches(path.toString()));
		}
	}
}
