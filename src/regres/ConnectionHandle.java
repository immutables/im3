package io.immutables.regres;

import io.immutables.meta.Null;

import java.sql.Connection;
import java.sql.SQLException;

final class ConnectionHandle implements ConnectionProvider.Handle {
	private static final ThreadLocal<Connection> openedConnection = new ThreadLocal<>();
	private final ConnectionProvider provider;
	private final boolean recycleOnClose;

	private final Connection connection;

	private ConnectionHandle(
			ConnectionProvider provider, Connection connection, boolean recycleOnClose) {
		this.provider = provider;
		this.connection = connection;
		this.recycleOnClose = recycleOnClose;
	}

	@Override
	public Connection connection() {
		return connection;
	}

	@Override
	public void close() throws SQLException {
		if (recycleOnClose) {
			openedConnection.remove();
			provider.recycle(connection);
		}
	}

	static ConnectionHandle get(ConnectionProvider provider) throws SQLException {
		@Null Connection existing = openedConnection.get();
		if (existing != null) {
			return new ConnectionHandle(provider, existing, false);
		}
		Connection newOne = provider.get();
		openedConnection.set(newOne);
		return new ConnectionHandle(provider, newOne, true);
	}
}
