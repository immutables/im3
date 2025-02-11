package io.immutables.build;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JustWorkspace implements Workspace {
  private final Path root;

  private final Lock lock = new ReentrantLock();
  private final Map<String, Action> actions = new HashMap<>();
  private final Map<ModuleName, ModuleUnit> modules = new LinkedHashMap<>();
  private final Map<ModuleName, TargetExecutor> executors = new HashMap<>();
  private final Map<Class<?>, Object> tools = new HashMap<>();

  public JustWorkspace(Path root) {
    this.root = root;
  }

  @Override public Path root() {
    return root;
  }

  @Override public void addModule(ModuleUnit module) {
    var name = module.name();
    lock.lock();
    try {
      if (modules.containsKey(name)) {
        throw new IllegalArgumentException("Duplicate module '%s'".formatted(name));
      }
      modules.put(name, module);
    } finally {
      lock.unlock();
    }
  }

  @Override public void addAction(String identifier, Action action) {
    lock.lock();
    try {
      if (actions.containsKey(identifier)) {
        throw new IllegalArgumentException("Duplicate action '%s'".formatted(identifier));
      }
      actions.put(identifier, action);
    } finally {
      lock.unlock();
    }
  }

  @Override public <T> void addTool(Class<T> type, T instance) {
    lock.lock();
    try {
      tools.put(type, instance);
    } finally {
      lock.unlock();
    }
  }

  @Override public <T> Optional<T> toolOf(Class<T> type) {
    lock.lock();
    try {
      return Optional.ofNullable(type.cast(tools.get(type)));
    } finally {
      lock.unlock();
    }
  }

  @Override public Optional<Action> actionBy(String identifier) {
    lock.lock();
    try {
      return Optional.ofNullable(actions.get(identifier));
    } finally {
      lock.unlock();
    }
  }

  @Override public List<ModuleUnit> modules() {
    lock.lock();
    try {
      return List.copyOf(modules.values());
    } finally {
      lock.unlock();
    }
  }

  @Override public Optional<ModuleUnit> moduleBy(ModuleName name) {
    lock.lock();
    try {
      return Optional.ofNullable(modules.get(name));
    } finally {
      lock.unlock();
    }
  }

  @Override public TargetExecutor executorFor(ModuleName name) {
    lock.lock();
    try {
      return executors.computeIfAbsent(name, k -> new TargetExecutionPerModule(k.name()));
    } finally {
      lock.unlock();
    }
  }

  @Override public Path buildOutputDirFor(ModuleName name) {
    return root.resolve(".build", name.name());
  }
}
