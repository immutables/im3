package io.immutables.declaration.http;

/**
 * Superinterface and grouping umbrella interface for commonly used standard 4xx,
 * so called, client errors.
 */
// @formatter:off
public abstract class ClientException extends ReturnException {
	@Status(400) public static final class BadRequest extends ClientException {}
	@Status(401) public static final class Unauthorized extends ClientException {}
	@Status(403) public static final class Forbidden extends ClientException {}
	@Status(404) public static final class NotFound extends ClientException {}
	@Status(405) public static final class MethodNotAllowed extends ClientException {}
	@Status(406) public static final class NotAcceptable extends ClientException {}
	@Status(409) public static final class Conflict extends ClientException {}
	@Status(410) public static final class Gone extends ClientException {}
	@Status(418) public static final class ImATeaPot extends ClientException {}
	@Status(422) public static final class UnprocessableEntity extends ClientException {}
	// for any other 4xx error, will have specific status code initialized from response
	public static final class Undeclared extends ClientException {}
}
