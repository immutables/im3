package io.immutables.regres.test;

import io.immutables.codec.Jsons;
import io.immutables.regres.SqlAccessor;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface Sample extends SqlAccessor {
	@UpdateCount
	long[] createTable() throws SQLException;

	@UpdateCount
	int[] insertValues() throws SQLException;

	@UpdateCount
	int insertValuesAndSelect() throws SQLException;

	List<Map<String, String>> insertValuesAndSelectSkippingUpdateCount() throws SQLException;

	// TODO verify
	List<Map<String, String>> multipleSelectFail() throws SQLException, IOException;

	void multipleSelectVoid() throws SQLException;

	@Single
	Map<String, String> selectSingle() throws SQLException;

	@Single(optional = true)
	Optional<Map<String, String>> selectEmpty() throws SQLException;

	@Single
	@Column(index = 1)
	String selectSingleColumn() throws SQLException;

	@Single(ignoreMore = true)
	@Column
	int selectFirstColumnIgnoreMore() throws SQLException;

	@Column("b")
	List<String> selectColumns() throws SQLException;

	@Column("c")
	@Single(ignoreMore = true)
	Jsons<List<Integer>> selectJsonbColumn() throws SQLException;

	@UpdateCount
	long dropTable() throws SQLException;

	@Single
	@Column
	String selectConcatSimple(@Named("a") String a1, @Named("b") String b2, @Named("c") String c3);

	@Single
	@Column
	String selectConcatSpread(@Spread Map<String, String> map);

	@Single
	@Column
	String selectConcatSpreadPrefix(
			@Spread Map<String, String> map1,
			@Spread(prefix = "u.") Map<String, String> map2,
			@Named("d") String d);

	void createTableForBatch() throws SQLException;

	@UpdateCount
	int[] insertBatch(@Named("a") @Batch List<String> a, @Named("b") int b) throws SQLException;

	@UpdateCount
	int[] insertBatchSpread(@Spread Map<String, String> m, @Named("b") @Batch int... values) throws SQLException;

	void dropTableForBatch();

	@Column
	List<String> selectFromBatch();

  @Single
  @Column
  String insertAndGetJsonb(@Named("map") Jsons<Map<String, Integer>> map);

	@Single
	AaBbCcc selectAutoNamingConvention();

	List<Bu> selectBuRecords();

	void createTypes();

	void insertTypes(@Spread FancyTypes types);

	@Column
	List<UUID> readUuid();

	@Single
	FancyTypes readFancyTypes();

	void addNested(@Spread Nested n, @Named("a") int a);//@Named("nested") Nested nested,

	@Single
	Nested selectNested();

	record FancyTypes(
			UUID id,
			OffsetDateTime dt,
			Instant ts,
			Jsons<String> jb
	) {}

	record Nested(
		int a,
		String b
		//Map<String, Integer> c
	) {}
}
