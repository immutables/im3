package io.immutables.regres;

/**
 * Problems which caused by illegal or mismatching declaration
 * of access interface or underlying SQL script resource. This includes wrong
 * usage of annotations. Most of these problems will manifest at the time of repository
 * object creation, when SQL resource is read and matched to access interface,
 * but some may be thrown later when such errors are only detected during execution.
 * Most of the actual SQL execution problems and syntax problems will be thrown as
 * {@link java.sql.SQLException} if declared in throws clause of data access methods
 * or
 */
public final class WrongDeclaration extends RuntimeException {
	WrongDeclaration(String message) { super(message); }

	WrongDeclaration(String message, Throwable cause) { super(message, cause); }
}
