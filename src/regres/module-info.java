module io.immutables.regres {
	requires static io.immutables.meta;

	requires io.immutables.common;
	requires io.immutables.codec;

	// optional dependency on postgresql to refine exception
	// messages if we're dealing with PostgreSQL database,
	// and that would be the case most of the time
	requires static org.postgresql.jdbc;

	requires com.fasterxml.jackson.core;
	requires java.sql;

	exports io.immutables.regres;
}
