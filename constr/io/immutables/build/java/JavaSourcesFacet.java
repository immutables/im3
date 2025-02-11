package io.immutables.build.java;

import io.immutables.build.Facet;
import io.immutables.build.Target;
import java.nio.file.Path;
import java.util.List;

public interface JavaSourcesFacet extends Facet {
  Path dir();
  Path moduleInfoFile();
  Target<List<Path>> sources();
}
