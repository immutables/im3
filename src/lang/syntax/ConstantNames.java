package io.immutables.lang.syntax;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

class ConstantNames {
	static Map<Integer, String> discover(Class<?> host) {
		return discover(host, t -> t == int.class, o -> (Integer) o);
	}

	static Map<Short, String> discoverTerms(Class<?> host) {
		return discover(host, t -> t == short.class, o -> (Short) o);
	}

	static <N extends Number> Map<N, String> discover(
		Class<?> host,
		Predicate<Class<?>> typeMatcher,
		Function<Object, N> extract) {

		var names = new HashMap<N, String>();
		assert host.isInterface();
		for (var f : host.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && typeMatcher.test(f.getType())) {
				try {
					names.put(extract.apply(f.get(null)), f.getName());
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}
		}
		return names;
	}
}
