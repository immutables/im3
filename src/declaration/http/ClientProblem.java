package dev.declaration.http;

/**
 * Superinterface and grouping umbrella interface for commonly used standard 4xx,
 * so called, client errors.
 */
// @formatter:off
public abstract class ClientProblem extends ReturnException {
	@Status(400) public static final class BadRequest extends ClientProblem {}
	@Status(401) public static final class Unauthorized extends ClientProblem {}
	@Status(403) public static final class Forbidden extends ClientProblem {}
	@Status(404) public static final class NotFound extends ClientProblem {}
	@Status(405) public static final class MethodNotAllowed extends ClientProblem {}
	@Status(406) public static final class NotAcceptable extends ClientProblem {}
	@Status(409) public static final class Conflict extends ClientProblem {}
	@Status(410) public static final class Gone extends ClientProblem {}
	@Status(418) public static final class ImATeaPot extends ClientProblem {}
	@Status(422) public static final class UnprocessableEntity extends ClientProblem {}
	// for any other 4xx error, will have specific status code initialized from response
	public static final class Undeclared extends ClientProblem {}
}
