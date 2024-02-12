package io.immutables.regres;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.NameIndex;
import io.immutables.codec.Out;
import io.immutables.meta.Null;
import java.lang.reflect.Type;
import java.util.*;
import static java.util.Objects.requireNonNull;

record MethodProfile(
	String name,
	OptionalInt batchParameter,
	boolean returnsUpdateCount,
	boolean extractColumn,
	List<ParameterProfile> parameters,
	Map<String, ParameterProfile> parametersByName,
	Optional<Codec<Object, In, Out>> returnTypeCodec,
	Type returnType
) {
	NameIndex parameterIndex() {
		return NameIndex.known(parameters().stream()
			.map(ParameterProfile::name)
			.toArray(String[]::new));
	}

	static class Builder {
		@Null String name;
		int batchParameter = ABSENT;
		boolean returnUpdateCount;
		boolean extractColumn;
		// this keeps parameter in order
		List<ParameterProfile> parameters = new ArrayList<>();
		Map<String, ParameterProfile> parametersByName = new HashMap<>();
		@Null Type returnType;
		@Null Codec<Object, In, Out> returnTypeCodec;

		MethodProfile build() {
			return new MethodProfile(
				requireNonNull(name),
				batchParameter == ABSENT
					? OptionalInt.empty()
					: OptionalInt.of(batchParameter),
				returnUpdateCount,
				extractColumn,
				List.copyOf(parameters),
				Map.copyOf(parametersByName),
				Optional.ofNullable(returnTypeCodec),
				requireNonNull(returnType));
		}
	}

	private static final int ABSENT = -1;
}
