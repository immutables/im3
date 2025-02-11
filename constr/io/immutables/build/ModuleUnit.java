package io.immutables.build;

import java.util.Optional;

public interface ModuleUnit {
  ModuleName name();
  <T extends Facet> Optional<T> facet(Class<T> type);
  Optional<Action> action(String action);
}
