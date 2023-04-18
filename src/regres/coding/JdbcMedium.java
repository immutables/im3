package io.immutables.regres.coding;

import io.immutables.codec.Medium;

public final class JdbcMedium {
	private JdbcMedium() {}

	public static final Medium<ResultSetIn, PreparedStatementOut> Jdbc = new Medium<>() {
		public String toString() {
			return JdbcMedium.class.getSimpleName();
		}
	};
}
