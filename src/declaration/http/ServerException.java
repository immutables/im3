package io.immutables.declaration.http;

/**
 * Superinterface and grouping umbrella interface for commonly used standard 5xx,
 * so called, server errors.
 */
// @formatter:off
public abstract class ServerException extends ReturnException {
	@Status(500) public static final class InternalServerError extends ServerException {}
	@Status(501) public static final class NotImplemented extends ServerException {}
	@Status(502) public static final class BadGateway extends ServerException {}
	@Status(503) public static final class NotAvailable extends ServerException {}
	@Status(504) public static final class GatewayTimeout extends ServerException {}
	// for any other 5xx error, will have specific status code initialized from response
	public static final class Undeclared extends ServerException {}
}
