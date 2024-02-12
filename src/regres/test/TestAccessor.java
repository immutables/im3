package io.immutables.regres.test;

import io.immutables.codec.Jsons;
import io.immutables.regres.Regresql;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestAccessor extends Base {
	private static final Sample sample = Regresql.create(Sample.class, codecs, connections);

	@Test
	public void results() throws Exception {
		that(sample.createTable()).isOf(0L, 0L);
		that(sample.insertValues()).isOf(1, 1, 1);
		that(sample.insertValuesAndSelect()).is(2);
		that(sample.insertValuesAndSelectSkippingUpdateCount()).isEmpty();
		that(sample::multipleSelectFail).thrown(Exception.class);
		sample.multipleSelectVoid(); // no exceptions
		Map<String, String> m1 = sample.selectSingle();
		that(m1.keySet()).hasOnly("a", "b", "c");
		that(m1.values()).hasOnly("1", "A", "[1]");
		that(sample.selectEmpty()).isEmpty();
		that(sample.selectSingleColumn()).is("C");
		that(sample.selectFirstColumnIgnoreMore()).is(1);
		that(sample.selectColumns()).isOf("A", "B", "C", "D", "E", "F");
		that(sample.selectJsonbColumn().content()).isOf(1);
		that(sample.dropTable()).is(0);
	}

	@Test
	public void parameters() {
		that(sample.selectConcatSimple("a", "b", "c")).is("abc");
		that(sample.selectConcatSpread(Map.of("a", "1", "b", "2", "c", "3"))).is("123");
		that(sample.selectConcatSpreadPrefix(Map.of("a", "1"), Map.of("b", "2", "c", "3"), "4"))
				.is("1234");
	}

	@Test
	public void batch() throws SQLException {
		sample.createTableForBatch();
		that(sample.insertBatch(List.of("X", "Y", "Z"), 0)).isOf(1, 1, 1);
		that(sample.insertBatchSpread(Map.of("a", "U"), 1, 2, 3)).isOf(1, 1, 1);
		that(sample.selectFromBatch()).hasOnly("X-0", "Y-0", "Z-0", "U-1", "U-2", "U-3");
		sample.dropTableForBatch();
	}

	@Test
	public void jsonb() throws SQLException {
		sample.createTable();
		String jsonb = sample.insertAndGetJsonb(new Jsons<>(Map.of("a", 1)));
		that(jsonb).is("{\"a\": 1}"); // mind formatting
		sample.dropTable();
	}

	@Test
	public void selectNaming() throws SQLException {
		var abc = sample.selectAutoNamingConvention();
		that(abc.aA()).is("A");
		that(abc.bBB()).is("B");
		that(abc.cCc()).is(3);
	}

	@Test
	public void selectBuRecord() throws SQLException {
		sample.createTable();
		sample.insertValues();
		List<Bu> bs = sample.selectBuRecords();
		that(bs).hasSize(3);
		that(bs.get(0).a()).is(1);
		that(bs.get(1).a()).is(2);
		that(bs.get(2).a()).is(3);
		that(bs.get(0).b()).is("A");
		that(bs.get(1).b()).is("B");
		that(bs.get(2).b()).is("C");
		that(bs.get(0).c().content()).isOf(1);
		that(bs.get(1).c().content()).isOf(2);
		that(bs.get(2).c().content()).isOf(3);
		sample.dropTable();
	}

	@Test
	public void jdbcTypes() {
		sample.createTypes();
		sample.insertTypes(new Sample.FancyTypes(
				UUID.randomUUID(),
				OffsetDateTime.now(),
				Instant.now(),
				new Jsons<>("jsonb")));
		List<UUID> ids = sample.readUuid();
		that(ids).notEmpty();
		var types = sample.readFancyTypes();
	}
}
