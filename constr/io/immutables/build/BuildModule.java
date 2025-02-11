package io.immutables.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class BuildModule implements ModuleUnit {
  private final ModuleName name;
  private final List<Facet> facets = new ArrayList<>();
  private final Map<String, Action> actions = new HashMap<>();

  protected BuildModule(ModuleName name) {
    this.name = name;
  }

  @Override public final ModuleName name() {
    return name;
  }

  public final BuildModule addFacet(Facet facet) {
    facets.add(facet);
    return this;
  }

  public final BuildModule addAction(String identifier, Action impl) {
    if (actions.containsKey(identifier)) {
      throw new IllegalArgumentException(
          "Actions should be unique, action identifier %s is already used"
              .formatted(identifier));
    }
    actions.put(identifier, impl);
    return this;
  }

  @Override public Optional<Action> action(String identifier) {
    return Optional.ofNullable(actions.get(identifier));
  }

  @Override public final <T extends Facet> Optional<T> facet(Class<T> type) {
    for (var t : facets) {
      if (type.isInstance(t)) {
        return Optional.of(type.cast(t));
      }
    }
    return Optional.empty();
  }
}
