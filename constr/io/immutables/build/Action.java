package io.immutables.build;

import java.io.PrintStream;
import java.util.List;

public interface Action {
  boolean perform(PrintStream out, PrintStream err, List<String> arguments);
}
