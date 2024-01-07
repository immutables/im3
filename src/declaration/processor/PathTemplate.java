package dev.declaration.processor;

import io.immutables.meta.Null;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathTemplate {
	public final String uri;
	public final String path;
	public final Map<String, Parameter> parameters;

	private PathTemplate(String uri, String path, Map<String, Parameter> parameters) {
		this.uri = uri;
		this.path = path;
		this.parameters = Map.copyOf(parameters);
	}

	public String toString() {
		return uri;
	}

	public boolean equals(Object obj) {
		return obj instanceof PathTemplate p && uri.equals(p.uri);
	}

	public int hashCode() {
		return uri.hashCode();
	}

	public static PathTemplate from(String uri) {
		var parameters = new HashMap<String, Parameter>();
		var path = collectFromPath(uri, parameters);
		collectFromQuery(uri, parameters);
		return new PathTemplate(uri, path, parameters);
	}

	private static String collectFromPath(String uri, Map<String, Parameter> parameters) {
		int queryAt = uri.indexOf('?');
		if (queryAt >= 0) {
			uri = uri.substring(0, queryAt);
		}
		var matcher = parameterPattern.matcher(uri);
		while (matcher.find()) {
			var name = matcher.group(1);
			parameters.put(name, new Parameter(name, name, Parameter.Kind.Path, null));
		}
		return uri;
	}

	// here and in other routines we assume at least somewhat sane syntax, so
	// better, more precise parsing and validation is probably needed
	private static void collectFromQuery(String uri, Map<String, Parameter> parameters) {
		int queryAt = uri.indexOf('?');
		if (queryAt >= 0 && queryAt + 1 < uri.length()) {
			var queryPart = uri.substring(queryAt + 1);
			for (var q : queryPart.split("&")) {
				int indexOfEquals = q.indexOf('=');
				if (indexOfEquals >= 0) {
					var k = q.substring(0, indexOfEquals);
					var v = q.substring(indexOfEquals + 1);
					String name;
					String httpName;
					@Null String value;
					Matcher matcher = parameterPattern.matcher(v);
					if (matcher.matches()) {
						// case: ?a={b}&t={w}
						// parameter name - http name,
						// parameter value - variable capture, specifies name
						name = matcher.group(1);
						httpName = k;
						value = null;
					} else {
						// case: ?a=1&b=
						// parameter name - name and http name
						// parameter value - some inline value
						name = k;
						httpName = k;
						value = v;
					}
					parameters.put(name, new Parameter(name, httpName, Parameter.Kind.Query, value));
				} else {
					// case: ?a&b
					// only parameter names, no values
					parameters.put(q, new Parameter(q, q, Parameter.Kind.Query, null));
				}
			}
		}
	}

	public String with(List<Declaration.FixedQuery> queries) {
		if (queries.isEmpty()) return path;

		var b = new StringBuilder(path).append('?');
		for (var q : queries) {
			b.append(q.name());
			if (q.value() != null) {
				b.append('=').append(q.value());
			}
		}
		return b.toString();
	}

	public record Parameter(String name, String httpName, Kind kind, @Null String value) {
		enum Kind {
			Path,
			Query
		}
	}

	private static final Pattern parameterPattern =
		Pattern.compile("\\{\\s*([a-zA-Z][0-9a-zA-Z]*)\\s*}");
}
