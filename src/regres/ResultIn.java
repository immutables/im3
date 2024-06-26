package io.immutables.regres;

import io.immutables.codec.Token;
import io.immutables.codec.AtPath;
import io.immutables.codec.In;
import io.immutables.codec.NameIndex;
import io.immutables.meta.Null;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Structured codec input read from JDBC result set.
 */
public class ResultIn extends In {
	private final ResultSet results;
	private final int columnCount;
	private final String[] names;
	private final int[] types;
	private final Object[] values;
	private final int[] indexes;

	private Token peek = Token.Array;
	private int atRow = -1;
	private int atColumn = -1;
	private @Null String currentName;
	private int currentType = Types.OTHER;

	// Will not close result set
	ResultIn(ResultSet results) throws SQLException {
		this.results = results;
		var meta = results.getMetaData();
		columnCount = meta.getColumnCount();

		names = new String[columnCount];
		values = new Object[columnCount];
		types = new int[columnCount];
		indexes = new int[columnCount];
		Arrays.fill(indexes, NameIndex.UNKNOWN);

		for (int i = 0; i < columnCount; i++) {
			names[i] = meta.getColumnLabel(i + 1);
			types[i] = meta.getColumnType(i + 1);
		}
	}

	private @Null NameIndex currentNames;

	private void configureNames(NameIndex names) {
		// check by reference if it's the same index we've already initialized
		if (currentNames != names) {
			currentNames = names;
			for (int i = 0; i < indexes.length; i++) {
				String columnLabel = this.names[i];
				// first trying to map verbatim column names
				int index = names.index(columnLabel);
				if (index == NameIndex.UNKNOWN) {
					index = names.index(snakeToCamel(columnLabel));
				}
				indexes[i] = index;
			}
		}
	}

	// should we use CaseFormat (from common)
	private static String snakeToCamel(String columnLabel) {
		var builder = new StringBuilder();
		boolean capitalizeNext = false;
		for (int i = 0; i < columnLabel.length(); i++) {
			char c = columnLabel.charAt(i);
			c = capitalizeNext
					? Character.toUpperCase(c)
					: Character.toLowerCase(c);

			if (c == '_') {
				capitalizeNext = true;
				// skipping _ character
			} else {
				capitalizeNext = false;
				builder.append(c);
			}
		}
		return builder.toString();
	}

	@Override
	public Token peek() throws IOException {
		return peek;
	}

	@Override
	public void endArray() throws IOException {
		if (peek != Token.ArrayEnd) unexpected("not at the end of result set");
		peek = Token.End;
	}

	@Override
	public void beginArray() throws IOException {
		if (peek != Token.Array) unexpected("not at beginning of result set");
		advanceRow();
	}

	@Override
	public void beginStruct(NameIndex names) throws IOException {
		if (peek != Token.Struct) {
			unexpected("not at beginning of result set");
			return;
		}
		configureNames(names);
		advanceColumn();
	}

	private void advanceColumn() {
		atColumn++;
		if (atColumn >= columnCount) {
			atColumn = -1;
			peek = Token.StructEnd;
		} else {
			peek = Token.Field;
		}
	}

	private void typeValue() {
		currentType = types[atColumn];
		Object v = values[atColumn];
		Token peek;
		if (v == null) peek = Token.Null;
		else if (v instanceof Integer) peek = Token.Int;
		else if (v instanceof Long) peek = Token.Long;
		else if (v instanceof Number) peek = Token.Float;
		else if (v instanceof Boolean b) peek = b == Boolean.TRUE ? Token.True : Token.False;
		else if (v instanceof String s) peek = Token.String;
		else peek = Token.Special;

		this.peek = peek;
	}

	@Override
	public int takeField() throws IOException {
		if (peek != Token.Field) unexpected("not at column");
		int field = indexes[atColumn];
		currentName = names[atColumn];
		typeValue();
		return field;
	}

	/**
	 * Get sql type, call before taking column
	 * @see java.sql.Types
	 */
	public int sqlType() {
		return currentType;
	}

	public String name() throws IOException {
		if (currentName == null) unexpected("no current name");
		return currentName;
	}

	@Override
	public void endStruct() throws IOException {
		if (peek != Token.StructEnd) unexpected("not at the end of the row");
		advanceRow();
	}

	private void advanceRow() {
		atColumn = -1;
		try {
			if (!results.next()) {
				peek = Token.ArrayEnd;
			} else {
				atRow++;
				peek = Token.Struct;
				for (int i = 0; i < columnCount; i++) {
					values[i] = results.getObject(i + 1);
				}
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public String takeString() throws IOException {
		if (!peek.isScalar()) {
			unexpected("cannot be read as a string");
			skip();
			return "";
		}
		String s = String.valueOf(values[atColumn]);
		advanceColumn();
		return s;
	}

	public Object takeSpecial() {
		Object v = values[atColumn];
		advanceColumn();
		return v;
	}

	@Override
	public void takeNull() throws IOException {
		if (peek != Token.Null) unexpected("not at null value");
		advanceColumn();
	}

	@Override
	public long takeLong() throws IOException {
		long l = switch (peek) { // @formatter:off
		case Int, Long, Float -> ((Number)values[atColumn]).longValue();
			case String -> Long.parseLong((String)values[atColumn]);
			default -> unexpected("not at int value", Long.MIN_VALUE);
		};
		advanceColumn();
		return l;
	}

	@Override
	public int takeInt() throws IOException {
		int i = switch (peek) {
			case Int, Long, Float -> ((Number)values[atColumn]).intValue();
			case String -> Integer.parseInt((String)values[atColumn]);
			default -> unexpected("not at int value", Integer.MIN_VALUE);
		};
		advanceColumn();
		return i;
	}

	@Override
	public double takeDouble() throws IOException {
		double d = switch (peek) {
			case Int, Long, Float -> ((Number) values[atColumn]).doubleValue();
			case String -> Double.parseDouble((String) values[atColumn]);
			default -> unexpected("not at double value", Double.NaN);
		};
		advanceColumn();
		return d;
	}

	@Override
	public boolean takeBoolean() throws IOException {
		boolean b = switch (peek) {
			case True -> true;//(boolean) values[atColumn]
			case False -> false;
			case Int, Long, Float -> ((Number) values[atColumn]).intValue() != 0;
			case String -> Boolean.parseBoolean((String) values[atColumn]);
			default -> values[atColumn] != null;
		};
		advanceColumn();
		return b;
	}

	@Override
	public void skip() throws IOException {
		if (peek == Token.Struct) {
			advanceRow();
		} else if (peek == Token.Array) {
			peek = Token.End;
		} else {
			advanceColumn();
		}
	}

	@Override
	public AtPath path() {
		AtPath path = AtPath.Root.Root;
		if (atRow >= 0) path = new AtPath.ElementAt(path, atRow);
		if (atColumn >= 0) path = new AtPath.FieldOf(path, names[atColumn]);
		return path;
	}

	@Override
	public boolean hasNext() {
		// field (column) within struct (row)
		// or struct (row) within array (of results)
		return peek == Token.Field || peek == Token.Struct;
	}

	// TODO revise as a whole hierarchy
	private void unexpected(String message) throws IOException {
		throw new IOException(message);
	}

	// TODO revise
	private <V> V unexpected(String message, V substitute) throws IOException {
		unexpected(message);
		return substitute;
	}


	@Override
	public Buffer takeBuffer() throws IOException {
		throw new UnsupportedOperationException("yet");
	}

	@Override
	public NameIndex index(String... known) {
		return NameIndex.known(known);
	}

	@Override
	public int takeString(NameIndex names) throws IOException {
		return names.index(takeString());
	}
}
