package io.immutables.codec.record;

import io.immutables.codec.*;
import io.immutables.meta.Null;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;

final class InlineRecordCodec extends CaseCodec<Object, In, Out> implements Expecting {
	private final Codec<Object, In, Out> componentCodec;
	private final Constructor<?> canonicalConstructor;
	private final Method accessor;
	private final Type type;

	InlineRecordCodec(Type type, Class<?> raw, RecordComponent component, Lookup<In, Out> lookup) {
		this.type = type;
		var arguments = Types.mapArguments(raw, type);
		var componentType = Types.resolveArguments(component.getGenericType(), arguments);

		componentCodec = lookup.get(componentType);
		canonicalConstructor = Reflect.getCanonicalConstructor(raw);
		accessor = component.getAccessor();
	}

	public void encode(Out out, Object instance) throws IOException {
		componentCodec.encode(out, Reflect.getValue(accessor, instance));
	}

	public @Null Object decode(In in) throws IOException {
		@Null Object decode = componentCodec.decode(in);
		if (in.wasInstanceFailed()) return null;
		return Reflect.newInstance(canonicalConstructor, decode);
	}

	public boolean mayConform(In in) throws IOException {
		if (componentCodec instanceof CaseCodec<Object, In, Out> c) {
			return c.mayConform(in);
		}
		if (componentCodec instanceof Expecting e) {
			return e.expects(in.peek());
		}
		// well, maybe component codec could still, possibly, work,
		// but we will not guess and bail out
		return false;
	}

	public boolean expects(In.At first) {
		return componentCodec instanceof Expecting e && e.expects(first);
	}

	public String toString() {
		return getClass().getSimpleName() + "<" + type.getTypeName() + ">";
	}
}
