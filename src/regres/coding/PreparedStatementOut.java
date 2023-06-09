package io.immutables.regres.coding;

import io.immutables.codec.NameIndex;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static java.util.Objects.requireNonNull;

final class PreparedStatementOut extends Out {
	private static final Object MASKED_NULL = new Object();

	private enum SpreadState {
		Expect, Doing, None
	}

	private final NameIndex parameterIndex;
	private final Map<String, Object> values = new HashMap<>();

	private SpreadState spreading = SpreadState.None;
	private String prefix = "";
	private String parameter = "";
	private @Null NameIndex spreadingIndex;

	PreparedStatementOut(NameIndex parameterIndex) {
		this.parameterIndex = parameterIndex;
	}

	@Null Object get(String name) throws IOException {
		Object v = values.get(name);
		if (v == null) unexpected("No value for placeholder :" + name);
		if (v == MASKED_NULL) return null;
		return v;
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
			unexpected("Parameter at %s uses nested structure. Instead use @Spread or JSON conversion"
				.formatted(path()));
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
		var names = spreading == SpreadState.Doing ? requireNonNull(spreadingIndex) : parameterIndex;
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
		unexpected("Parameter at %s uses nested array. Use special conversion or JSON instead"
			.formatted(path()));
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

	public void putString(NameIndex names, int index) throws IOException {
		values.put(parameter, names.name(index));
	}

	public String path() {
		return parameter;
	}

	private void unexpected(String message) throws IOException {
		throw new IOException(message);
	}
}
