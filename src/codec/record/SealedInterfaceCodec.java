package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import io.immutables.meta.NullUnknown;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class SealedInterfaceCodec extends DefaultingCodec<Object, In, Out> {
	private static final MetadataProvider metadata = Providers.metadata();

	private final Map<Class<?>, PerCase> cases = new HashMap<>();
	private final @Null Member reflectiveDefault;
	private final Type type;
	private final Class<?> raw;

	record PerCase(Codec<Object, In, Out> codec, @Null CaseTag tag) {}

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
			@Null var tag = metadata.findCaseTag(c, raw);
			cases.put(c, new PerCase(lookup.get(subtype), tag));
		}

		this.reflectiveDefault = metadata.findReflectiveDefault(raw);
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
		@Null var c = cases.get(instance.getClass());
		if (c == null) throw new RuntimeException(
				"Unexpected subclass of %s of %s".formatted(instance.getClass(), raw));

		if (c.codec instanceof CaseCodec<Object, In, Out> codec) {
			codec.encode(out, instance, c.tag);
		} else {
			c.codec.encode(out, instance);
		}
	}

	public @NullUnknown Object decode(In in) throws IOException {
		In.Buffer buffer = in.takeBuffer();

		@Null Codec<Object, In, Out> actualCodec = null;

		for (var c : cases.values()) {
			if (c.codec instanceof CaseCodec<Object, In, Out> caseCodec) {
				if (caseCodec.mayConform(buffer.in(), c.tag)) {
					actualCodec = caseCodec;
					break;
				}
			}
		}

		if (actualCodec != null) {
			return actualCodec.decode(buffer.in());
		}

		in.noMatchingCase(type);
		return in.problems.unreachable();
	}

	public boolean providesDefault() {
		return reflectiveDefault != null;
	}

	public @Null Object getDefault(In in) throws IOException {
		if (reflectiveDefault != null)
			try {
				return Reflect.constructValue(reflectiveDefault);
			} catch (RuntimeException exception) {
				in.cannotInstantiate(type, exception.getMessage());
				return in.problems.unreachable();
			}

		return null;
	}

	public String toString() {
		return getClass().getSimpleName() + "<" + type.getTypeName() + ">";
	}
}
