package io.immutables.regres;

/**
 * Runtime exception wrapper used for {@link java.sql.SQLException} if access
 * method doesn't declare this checked exception in throws clause.
 */
public final class SqlException extends RuntimeException {
	SqlException(String message) {super(message);}
	SqlException(String message, Exception cause) {
		super(message, cause);
	}
}
