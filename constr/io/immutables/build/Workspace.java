package io.immutables.build;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface Workspace {
  Path root();

  void addModule(ModuleUnit module);

  void addAction(String identifier, Action action);

  <T> void addTool(Class<T> type, T instance);

  <T> Optional<T> toolOf(Class<T> type);

  Optional<Action> actionBy(String identifier);

  List<ModuleUnit> modules();

  Optional<ModuleUnit> moduleBy(ModuleName name);

  TargetExecutor executorFor(ModuleName name);

  Path buildOutputDirFor(ModuleName name);
}
