package build;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

interface Http {
	HttpClient http = HttpClient.newHttpClient();

	static void fetch(URI uri, Path target) throws IOException, InterruptedException {
		var result = http.send(
			HttpRequest.newBuilder(uri).GET().build(),
			HttpResponse.BodyHandlers.ofFile(target));

		var body = result.body();
	}
}
