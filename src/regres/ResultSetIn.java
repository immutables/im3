package io.immutables.regres;

import io.immutables.codec.In;
import io.immutables.codec.NameIndex;
import io.immutables.meta.Null;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import static com.fasterxml.jackson.core.JsonTokenId.ID_NO_TOKEN;
import static com.fasterxml.jackson.core.JsonTokenId.ID_STRING;

public class ResultSetIn extends In {
	private final ResultSet results;
	private final int columnCount;
	private final String[] names;
	private final Object[] values;
	private final int[] indexes;

	private At peek = At.Array;
	private int atRow = -1;
	private int atColumn = -1;
	private @Null String currentName;

	// Will not close result set
	ResultSetIn(ResultSet results) throws SQLException {
		this.results = results;
		ResultSetMetaData meta = results.getMetaData();
		columnCount = meta.getColumnCount();

		names = new String[columnCount];
		values = new Object[columnCount];
		indexes = new int[columnCount];
		Arrays.fill(indexes, NameIndex.UNKNOWN);

		for (int i = 0; i < columnCount; i++) {
			names[i] = meta.getColumnLabel(i + 1);
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
	public At peek() throws IOException {
		return peek;
	}

	@Override
	public void endArray() throws IOException {
		if (peek != At.ArrayEnd) unexpected("not at the end of result set");
		peek = At.End;
	}

	@Override
	public void beginArray() throws IOException {
		if (peek != At.Array) unexpected("not at beginning of result set");
		advanceRow();
	}

	@Override
	public void beginStruct(NameIndex names) throws IOException {
		if (peek != At.Struct) {
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
			peek = At.StructEnd;
		} else {
			peek = At.Field;
		}
	}

	private void typeValue() {
		Object v = values[atColumn];
		At peek;
		if (v == null) peek = At.Null;
		else if (v instanceof Integer) peek = At.Int;
		else if (v instanceof Long) peek = At.Long;
		else if (v instanceof Number) peek = At.Float;
		else if (v instanceof Boolean b) peek = b == Boolean.TRUE ? At.True : At.False;
		else if (v instanceof String s) peek = At.String;
		else peek = At.Special;

		this.peek = peek;
	}

	@Override
	public int takeField() throws IOException {
		if (peek != At.Field) unexpected("not at column");
		int field = indexes[atColumn];
		currentName = names[atColumn];
		typeValue();
		return field;
	}

	public String name() throws IOException {
		if (currentName == null) unexpected("no current name");
		return currentName;
	}

	@Override
	public void endStruct() throws IOException {
		if (peek != At.StructEnd) unexpected("not at the end of the row");
		advanceRow();
	}

	private void advanceRow() {
		atColumn = -1;
		try {
			if (!results.next()) {
				peek = At.ArrayEnd;
			} else {
				atRow++;
				peek = At.Struct;
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

	public Object takeSpecial() throws IOException {
		Object v = values[atColumn];
		advanceColumn();
		return v;
	}

	@Override
	public void takeNull() throws IOException {
		if (peek != At.Null) unexpected("not at null value");
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
		if (peek == At.Struct) {
			advanceRow();
		} else if (peek == At.Array) {
			peek = At.End;
		} else {
			advanceColumn();
		}
	}

	@Override
	public String path() {
		return "$"
			+ (atRow >= 0 ? "[" + atRow + "]" : "")
			+ (atColumn >= 0 ? "." + names[atColumn] : "");
	}

	@Override
	public boolean hasNext() {
		// field (column) within struct (row)
		// or struct (row) within array (of results)
		return peek == At.Field || peek == At.Struct;
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
