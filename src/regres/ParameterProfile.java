package io.immutables.regres;

import io.immutables.codec.Codec;
import io.immutables.codec.In;
import io.immutables.codec.Out;
import java.lang.reflect.Type;
import java.util.Optional;

record ParameterProfile(
	String name,
	boolean batch,
	Optional<String> spread,
	Codec<Object, In, Out> codec,
	Type type
) {}
