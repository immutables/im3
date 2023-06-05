package build;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

record SourceModule(
	Path dir,
	Path relative,
	ModuleInfo moduleInfo
) implements ProvidingModule {

	@Override
	public String name() {
		return moduleInfo.name();
	}
}

public class Sources {
	private Sources() {}

	static final List<SourceModule> modules = new ArrayList<>();

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

		var projectDir = Path.of(".").toAbsolutePath();
		var moduleDir = dir.toAbsolutePath();
		var moduleInfo = Jms.parseModuleInfo(file, moduleDir, projectDir);

		modules.add(new SourceModule(dir, searchPath.relativize(dir), moduleInfo));
	}

	private static final int MAX_DEPTH = 30;
}
