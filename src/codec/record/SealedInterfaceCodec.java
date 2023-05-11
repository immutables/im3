package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class SealedInterfaceCodec extends DefaultingCodec<Object, In, Out> {
	private final Map<Class<?>, Codec<Object, In, Out>> subclassesCodecs = new HashMap<>();
	private final @Null Member reflectiveDefault;
	private final Type type;
	private final Class<?> raw;

	SealedInterfaceCodec(Type type, Class<?> raw, Lookup<In, Out> lookup) {
		this.type = type;
		this.raw = raw;
		var permittedSubclasses = raw.getPermittedSubclasses();

		Type[] arguments = Types.getArguments(type);
		if (arguments.length > 0) {
			checkTypeParametersMatchExactly(type, raw, permittedSubclasses);
		}

		for (var c : permittedSubclasses) {
			// this based on the checkTypeParametersMatchExactly check,
			// that type parameters of both interface and subtype record mush match exactly
			var subtype = arguments.length > 0 ? Types.newParameterized(c, arguments) : c;
			subclassesCodecs.put(c, lookup.get(subtype));
		}

		this.reflectiveDefault = Providers.metadata().findReflectiveDefault(raw);
	}

	private static void checkTypeParametersMatchExactly(
		Type type, Class<?> raw, Class<?>[] subclasses) {

		var interfaceParameters = raw.getTypeParameters();
		for (var subclass : subclasses) {
			TypeVariable<?>[] subclassParametes = subclass.getTypeParameters();
			ok:
			{
				if (interfaceParameters.length != subclassParametes.length) break ok;
				// this routine doesn't consider superinterfaces / intermediate interfaces
				// for sealed types, can those be sandwiched in practice?
				boolean foundInterface = false;
				for (var anInterface : subclass.getGenericInterfaces()) {
					if (Types.toRawType(anInterface) == raw) {
						if (!Arrays.equals(subclassParametes, Types.getArguments(anInterface))) break ok;
						foundInterface = true;
						break;
					}
				}
				// this is paranoidal bail-out, cannot be in practice, but let it break
				if (!foundInterface) break ok;
				// this subclass is ok, continue
				continue;
			}
			throw new IllegalArgumentException(
				"Type parameters mismatch in sealed interface (%s) and subclass (%s)"
					.formatted(type, subclass));
		}
	}

	public void encode(Out out, Object instance) throws IOException {
		@Null var subclassCodec = subclassesCodecs.get(instance.getClass());
		if (subclassCodec == null) throw new RuntimeException(
			"Unexpected subclass of %s of %s".formatted(instance.getClass(), raw));

		subclassCodec.encode(out, instance);
	}

	public @Null Object decode(In in) throws IOException {
		In.Buffer buffer = in.takeBuffer();

		@Null Codec<Object, In, Out> actualCodec = null;

		for (var c : subclassesCodecs.values()) {
			if (c instanceof CaseCodec<Object, In, Out> candidate) {
				if (candidate.mayConform(buffer.in())) {
					actualCodec = candidate;
					break;
				}
			}
		}

		if (actualCodec != null) {
			return actualCodec.decode(buffer.in());
		}

		in.failInstance();
		return null;
	}

	public boolean providesDefault() {
		return reflectiveDefault != null;
	}

	public @Null Object getDefault() {
		return reflectiveDefault != null
			? ReflectRecords.constructValue(reflectiveDefault)
			: null;
	}

	public String toString() {
		return getClass().getSimpleName() + "<" + type.getTypeName() + ">";
	}
}
