package io.immutables.build.java;

import io.immutables.build.ModuleName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public interface Repackage {

  static void mergeAutomaticModule(
      Path tmpDir, Path vendoredFile, ModuleName module, List<Path> jars) throws IOException {

    Files.createDirectories(tmpDir);

    var merged = Files.createTempFile(
        tmpDir,
        Repackage.class.getSimpleName() + "_",
        vendoredFile.getFileName().toString());

    try (var out = new ZipOutputStream(Files.newOutputStream(merged))) {
      var writtenEntries = new HashSet<>();

      for (var j : jars) {
        try (var in = new ZipFile(j.toFile())) {
          var entries = in.entries();
          while (entries.hasMoreElements()) {
            var next = entries.nextElement();
            var name = next.getName();

            if (writtenEntries.contains(name) || skippedEntry(name)) continue;

            writtenEntries.add(name);

            out.putNextEntry(new ZipEntry(name));
            // Any jar file must contain manifest as the first entry
            // Here we actually rely on that this entry will be the first
            // one in first jar
            if (name.equals(MANIFEST_ENTRY)) {
              var manifest = new Manifest(in.getInputStream(next));
              rewriteManifest(module, manifest).write(out);
              // We're effectively dropping all original attributes
            } else try (var ins = in.getInputStream(next)) {
              ins.transferTo(out);
            }
            out.closeEntry();
          }
        }
      }
    }
    // atomic move might not be supported
    Files.move(merged, vendoredFile, StandardCopyOption.REPLACE_EXISTING);
  }

  private static Manifest rewriteManifest(ModuleName module, Manifest manifest) {
    var mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
    var newManifest = new Manifest();
    newManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (mainClass != null) {
      newManifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
    }
    newManifest.getMainAttributes().putValue(MANIFEST_AUTOMATIC_MODULE_NAME, module.name());
    // skipping any other entries
    return newManifest;
  }

  private static boolean skippedEntry(String name) {
    return name.endsWith("module-info.class") // always skip module-info when repackaging
        || name.endsWith("/INDEX_LIST") // inhibitor fence
        || name.endsWith("/INDEX.LIST") // inhibitor fence
        || name.endsWith(".SF") // signatures
        || name.endsWith(".RSA") // signatures
        || name.endsWith(".DES") // signatures
        || name.startsWith("META-INF/maven/"); // maven metadata
  }

  String MANIFEST_ENTRY = "META-INF/MANIFEST.MF";
  String MANIFEST_AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";
}
