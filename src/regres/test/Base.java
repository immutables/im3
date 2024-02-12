package io.immutables.regres.test;

import io.immutables.codec.Jsons;
import io.immutables.codec.Medium;
import io.immutables.codec.Registry;
import io.immutables.codec.jackson.EmbeddedJson;
import io.immutables.codec.record.RecordsFactory;
import io.immutables.regres.ConnectionProvider;
import io.immutables.regres.JdbcCodecs;
import io.immutables.regres.JdbcMedium;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class Base {
	static final Registry codecs = new Registry.Builder()
		.add(new RecordsFactory())
		.add(JdbcCodecs.Instance, JdbcMedium.Jdbc)
		.add(EmbeddedJson.using(new JsonFactoryBuilder()
			.build()), Medium.Any, Jsons.class)
		.build();

	// Connection pool would do the same better I guess :)
	private static final AtomicReference<Connection> connectionRef = new AtomicReference<>();

	static final ConnectionProvider connections = new ConnectionProvider() {
		@Override
		public Connection get() {
			return connectionRef.getAcquire();
		}

		@Override
		public void recycle(Connection c) {
			connectionRef.setRelease(c);
		}
	};

	@BeforeClass
	public static void openConnection() throws SQLException {
		connectionRef.set(DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres"));
	}

	@AfterClass
	public static void closeConnection() throws SQLException {
		connectionRef.get().close();
	}
}
