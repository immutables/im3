package io.immutables.regres;

import io.immutables.codec.In;
import io.immutables.codec.Medium;
import io.immutables.codec.Out;

public final class JdbcMedium {
	private JdbcMedium() {}

	public static final Medium<ResultIn, StatementOut> Jdbc = new Medium<>() {
		public String toString() {
			return JdbcMedium.class.getSimpleName();
		}
	};

	// Same Jdbc medium, but re-casted as regular In/Out medium to simplify/eliminate casts
	// in some places
	@SuppressWarnings("unchecked")
	static final Medium<In, Out> Internal = (Medium<In, Out>) (Object) Jdbc;

}
