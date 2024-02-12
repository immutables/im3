package io.immutables.regres;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Lightweight interface abstracting datasources/any connection management,
 * as well as preparing connection, i.e. targeting it on specific schemas or
 * other session level setup. It's a breeze to implement using lambda calling
 * {@code () -> DriverManager.getConnection(uriString)}
 * with connection string or {@code DataSource::getConnection}, so
 * we don't even provide factories for these.
 */
public interface ConnectionProvider {
	/**
	 * Acquire new or pooled connection. Call {@link #recycle(Connection)} to close/or
	 * release it to the pool when done. Do not call {@link Connection#close()} directly.
	 */
	Connection get() throws SQLException;
	/**
	 * Recycles the connection: either close it or release to a pool.
	 * By using recycle method we don't force implementors to create connection wrappers which
	 * suppress close and override it to release a connection or do any tear down action.
	 * By default, we just call {@link Connection#close()}.
	 */
	default void recycle(Connection c) throws SQLException {
		c.close();
	}

	/**
	 * {@link AutoCloseable} thread-local connection handle for using with ARM-blocks.
	 */
	default Handle handle() throws SQLException {
		return ConnectionHandle.get(this);
	}

	interface Handle extends AutoCloseable {
		/** Current open connection. */
		Connection connection();

		/**
		 * Closes handle, implementing {@link AutoCloseable} protocol, narrowing
		 * thrown exception to {@link SQLException}
		 */
		void close() throws SQLException;
	}
}
