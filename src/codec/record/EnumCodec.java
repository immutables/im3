package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;

final class EnumCodec<E extends Enum<E>> extends CaseCodec<E, In, Out> implements Expecting {
	private final String[] constantNames;
	private final E[] constants;
	private final @Null E defaultConstant;
	private final Class<?> raw;

	// it's ok without volatile barrier
	private @Null NameIndex names;

	@SuppressWarnings("unchecked") // generic E here is for internal consistency, runtime checked
	EnumCodec(Class<?> raw) {
		this.raw = raw;
		assert raw.isEnum();

		var enumType = (Class<E>) raw;
		constants = enumType.getEnumConstants();
		constantNames = new String[constants.length];
		for (int i = 0; i < constants.length; i++) {
			var c = constants[i];
			assert i == c.ordinal();
			constantNames[i] = c.name();
		}

		defaultConstant = Providers.metadata().findDefaultConstant(enumType);
	}

	public void encode(Out out, E instance) throws IOException {
		if (names == null) names = out.index(constantNames);
		out.putString(names, instance.ordinal());
	}

	public @Null E decode(In in) throws IOException {
		if (names == null) names = in.index(constantNames);
		int f = in.takeString(names);
		if (f >= 0) return constants[f];
		else if (defaultConstant != null) return defaultConstant;
		// if no default, we declare instance failed
		in.cannotInstantiate(raw, "No such enum value '%s'".formatted(in.name()));
		return in.problems.unreachable();
	}

	public @Null E getDefault(In in) {
		return defaultConstant;
	}

	public boolean providesDefault() {
		return defaultConstant != null;
	}

	boolean mayConform(In in) throws IOException {
		if (names == null) names = in.index(constantNames);
		if (in.peek() == Token.String) {
			return in.takeString(names) >= 0;
		}
		return false;
	}

	public boolean expects(Token first) {
		return first == Token.String;
	}

	public String toString() {
		return getClass().getSimpleName() + "<" + raw.getTypeName() + ">";
	}
}
