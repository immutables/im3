package io.immutables.regres;

import io.immutables.meta.Null;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Lightweight interface abstracting datasources/any connection management, as well as preparing
 * connection, i.e. targeting it on specific schemas or other session level setup.
 * It's a breeze to implement using lambda calling
 * {@code () -> DriverManager.getConnection(uriString)}
 * with connection string or {@code DataSource::getConnection}. So easy we don't even provide
 * factories for these.
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
  default ConnectionHandle handle() throws SQLException {
    return ConnectionHandle.get(this);
  }

  final class ConnectionHandle implements AutoCloseable {
    private static final ThreadLocal<Connection> openedConnection = new ThreadLocal<>();
    private final ConnectionProvider provider;
    private final boolean recycleOnClose;

    public final Connection connection;

    private ConnectionHandle(ConnectionProvider provider, Connection connection, boolean recycleOnClose) {
      this.provider = provider;
      this.connection = connection;
      this.recycleOnClose = recycleOnClose;
    }

    @Override
    public void close() throws SQLException {
      if (recycleOnClose) {
        openedConnection.remove();
        provider.recycle(connection);
      }
    }

    private static ConnectionHandle get(ConnectionProvider provider) throws SQLException {
      @Null Connection existing = openedConnection.get();
      if (existing != null) {
        return new ConnectionHandle(provider, existing, false);
      }
      Connection aNewOne = provider.get();
      openedConnection.set(aNewOne);
      return new ConnectionHandle(provider, aNewOne, true);
    }
  }
}
