package io.immutables.lang.node;

import io.immutables.lang.node.Term.AboutInfo;
import io.immutables.meta.Null;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class TermsInfo {
  private final Map<Short, AboutInfo> byCode = new HashMap<>();
  private final Map<String, AboutInfo> bySymbol = new HashMap<>();

  public Optional<AboutInfo> about(String symbol) {
    return Optional.ofNullable(bySymbol.get(symbol));
  }

  public AboutInfo about(short term) {
    @Null AboutInfo about = byCode.get(term);
    if (about == null) throw new NoSuchElementException("No such term: " + term);
    return about;
  }

  public void registerSymbols(BiConsumer<String, Short> consumer) {
    byCode.forEach((code, about) -> consumer.accept(about.symbol(), code));
  }

  TermsInfo(Class<?> c) {
    assert c.isInterface();
    for (var f : c.getFields()) {
      int m = f.getModifiers();
      if (Modifier.isStatic(m) && f.getType() == short.class) {
        @Null Term.About annotation = f.getAnnotation(Term.About.class);
        if (annotation == null) continue;
        Short code;
        try {
          code = (Short) f.get(null);
        } catch (Exception e) {
          throw new AssertionError(e);
        }
        var symbol = annotation.as().intern();
        var name = f.getName().intern();
        var info = new AboutInfo(code, name, symbol, annotation.kind());
        @Null var duplicate = byCode.put(code, info);
        if (duplicate != null) {
          throw new AssertionError(
              "Term duplicate definition for code " + code + ": " + duplicate + " " + annotation);
        }
        if (!symbol.isEmpty()) {
          bySymbol.put(symbol, info);
        }
      }
    }
  }
}
