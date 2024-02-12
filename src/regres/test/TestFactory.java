package io.immutables.regres.test;

import io.immutables.regres.Regresql;
import io.immutables.regres.SqlException;
import io.immutables.regres.SqlFactory;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestFactory extends Base {
	static final SqlFactory factory = Regresql.factory(codecs, connections);

	record Val(int val) {}

	record Gnr<T>(T g) {}

	@Test public void selectSingle() {
		var s = factory.statement();
		var result = s.sql("select 12 as val").single(Val.class);

		that(result.val()).is(12);
	}

	@BeforeClass
	public static void setupTable() {
		int inserted = factory.transaction(() -> {
			factory.sql("""
				drop table if exists my_table;
				create table my_table(a int, b text);
				""").update();

			return factory.sql(
					"insert into my_table(a, b) values (:a, :b)", p -> p.batchSpread(
						new MyTable(1, "X"),
						new MyTable(2, "Y"),
						new MyTable(3, "Z")))
				.update();
		});
		assert inserted == 3;
	}

	@AfterClass
	public static void dropTable() {
		factory.sql("drop table my_table");
	}

	record MyTable(int a, String b) {}

	@Test public void selectList() {
		var list = factory.sql("""
			select * from my_table order by a
			""").list(MyTable.class);

		that(list.stream().map(MyTable::b)).isOf("X", "Y", "Z");
	}

	@Test public void selectFilteredList() {
		var list = factory.statement()
			.sql("select * from my_table where a > :a order by a", p -> p.bind("a", 1))
			.list(MyTable.class);

		that(list.stream().map(MyTable::b)).isOf("Y", "Z");
	}

	@Test public void selectFilteredFromParts() {
		var s = factory.statement();
		var result = s.sql("select * from my_table where b = ", s.params.next("X"))
			.single(MyTable.class);

		that(result.a).is(1);
		that(result.b).is("X");
	}

	@Test public void selectFirst() {
		var first = factory.sql("select * from my_table")
			.first(MyTable.class);

		that(first).isPresent();
	}

	@Test public void bindGeneric() {
		var first = factory.sql("select * from my_table where a > :g")
			.params(p -> p.spread(new Gnr<Integer>(1))
				.parameterizedType(Gnr.class, Integer.class))
			.first(MyTable.class);

		that(first).isPresent();
	}

	@Test public void selectSeparating() {
		var list = factory.sql("select * from my_table where a in (")
			.separating(", ", List.of(1, 2), (v, params) -> params.next(v))
			.sql(")")
			.list(MyTable.class);

		that(list.stream().map(MyTable::b)).isOf("X", "Y");
	}

	@Test public void selectIn() {
		var list = factory.sql("select * from my_table where b")
			.in("X", "Z")
			.list(MyTable.class);

		that(list.stream().map(MyTable::b)).isOf("X", "Z");
	}

	@Test public void failOnSelectMore() {
		that(() -> {
			factory.sql("select * from my_table").single(MyTable.class);
		}).thrown(SqlException.class);

		that(() -> {
			factory.sql("select * from my_table").optional(MyTable.class);
		}).thrown(SqlException.class);
	}
}
