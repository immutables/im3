package io.immutables.build;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public interface Http {
  void fetch(URI uri, Path target) throws IOException, InterruptedException;

  static void provide(Workspace workspace) {
    workspace.addTool(Http.class, new Http() {
      final HttpClient http = HttpClient.newHttpClient();

      @Override public void fetch(URI uri, Path target) throws IOException, InterruptedException {
        var result = http.send(
            HttpRequest.newBuilder(uri).GET().build(),
            HttpResponse.BodyHandlers.ofFile(target));

        var body = result.body();
      }
    });
  }
}
