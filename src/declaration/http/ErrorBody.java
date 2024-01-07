package dev.declaration.http;

import java.util.Optional;

/**
 * Standard simple error contains symbolic error code (alphanumerical id or some enum value).
 * With optional detailed information.
 */
public record ErrorBody(
	String error,
	Optional<String> detail
) {
	public ErrorBody(String error) {
		this(error, Optional.empty());
	}
}
