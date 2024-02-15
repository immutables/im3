package io.immutables.regres;

import io.immutables.codec.NameIndex;
import io.immutables.codec.Out;
import io.immutables.meta.Late;
import io.immutables.meta.Null;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static io.immutables.regres.Exceptions.methodLine;
import static java.util.Objects.requireNonNull;

public final class StatementOut extends Out {
	private enum SpreadState {
		Expect, Doing, None
	}

	private final MethodProfile profile;
	private final NameIndex parameterIndex;
	private final Map<String, Object> values = new HashMap<>();

	private SpreadState spreading = SpreadState.None;
	private String prefix = "";
	private String parameter = "";
	private @Null NameIndex spreadingIndex;

	StatementOut(MethodProfile profile, NameIndex parameterIndex) {
		this.profile = profile;
		this.parameterIndex = parameterIndex;
	}

	private record TypedValue(int sqlType, @Null Object value) {}

	private @Late ParameterProfile parameterProfile;

	void parameter(ParameterProfile p) {
		parameterProfile = p;
	}

	void spread(String prefix) {
		spreading = SpreadState.Expect;
		this.prefix = prefix;
	}

	@Override
	public void beginStruct(NameIndex f) throws IOException {
		if (spreading == SpreadState.Expect) {
			spreading = SpreadState.Doing;
			spreadingIndex = f;
		} else {
			onlyScalarAllowed();
		}
	}

	@Override
	public void endStruct() throws IOException {
		if (spreading == SpreadState.Doing) {
			spreading = SpreadState.None;
			prefix = "";
		} else {
			unexpected("Out of order end of struct at " + path());
		}
	}

	@Override
	public void putField(int field) {
		var names =
			spreading == SpreadState.Doing ? requireNonNull(spreadingIndex) : parameterIndex;
		putField(names.name(field));
	}

	public void putField(String name) {
		this.parameter = prefix + name;
	}

	public NameIndex index(String... known) {
		return NameIndex.known(known);
	}

	@Override
	public void endArray() throws IOException {
		unexpected("Out of order end of array at " + path());
	}

	@Override
	public void beginArray() throws IOException {
		onlyScalarAllowed();
	}

	private void onlyScalarAllowed() throws IOException {
		var what = spreading == SpreadState.None ? "Parameter" : "Field";
		var suggestion = spreading == SpreadState.None
			? "Use @Spread to fields, Jsons<T> or explicit conversion (toString() etc)"
			: "Use a string, a number, Jsons<T> or any type convertible to scalar";

		unexpected("%s `%s` cannot be converted to a scalar. %s%s"
			.formatted(what, parameter, suggestion, methodLine(profile, parameterProfile)));
	}

	@Override
	public void putInt(int i) {
		values.put(parameter, i);
	}

	@Override
	public void putLong(long l) {
		values.put(parameter, l);
	}

	@Override
	public void putDouble(double d) {
		values.put(parameter, d);
	}

	@Override
	public void putBoolean(boolean b) {
		values.put(parameter, b);
	}

	public void putSpecial(int sqlType, @Null Object value) {
		values.put(parameter, new TypedValue(sqlType, value));
	}

	public void putSpecial(Object o) {
		values.put(parameter, o);
	}

	@Override
	public void putNull() {
		values.put(parameter, MASKED_NULL);
	}

	@Override
	public void putString(String s) {
		values.put(parameter, s);
	}

	@Override
	public void putString(char[] chars, int offset, int length) {
		values.put(parameter, String.valueOf(chars, offset, length));
	}

	@Override
	public void putString(NameIndex names, int index) throws IOException {
		values.put(parameter, names.name(index));
	}

	public String path() {
		return parameter;
	}

	private void unexpected(String message) throws IOException {
		throw new SqlException(message);
	}

	void fillStatement(
		PreparedStatement statement,
		MethodSnippet snippet) throws SQLException, IOException {
		int i = 1;
		for (var placeholder : snippet.placeholders()) {
			@Null Object v = values.get(placeholder.name());
			if (v == null) {
				throw new WrongDeclaration("\n" + snippet.source().problemAt(
					placeholder.range(),
					"No parameter or value for placeholder :" + placeholder.name(),
					"Add parameter with such name or @Spread parameter having such " +
						"field/component\nAvailable parameters: " + String.join(", ",
						values.keySet())));
			}
			if (v == MASKED_NULL) {
				// do we need setNull with specific JDBC type?
				statement.setObject(i, null);
			} else if (v instanceof TypedValue typed) {
				statement.setObject(i, typed.value, typed.sqlType);
			} else {
				statement.setObject(i, v);
			}
			i++;
		}
	}

	private static final Object MASKED_NULL = new Object();
}
