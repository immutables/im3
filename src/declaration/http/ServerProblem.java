package dev.declaration.http;

/**
 * Superinterface and grouping umbrella interface for commonly used standard 5xx,
 * so called, server errors.
 */
// @formatter:off
public abstract class ServerProblem extends ReturnException {
	@Status(500) public static final class InternalServerError extends ServerProblem {}
	@Status(501) public static final class NotImplemented extends ServerProblem {}
	@Status(502) public static final class BadGateway extends ServerProblem {}
	@Status(503) public static final class NotAvailable extends ServerProblem {}
	@Status(504) public static final class GatewayTimeout extends ServerProblem {}
	// for any other 5xx error, will have specific status code initialized from response
	public static final class Undeclared extends ServerProblem {}
}
