package io.immutables.codec.test;

import io.immutables.codec.Types;
import java.util.List;
import org.junit.Test;
import static io.immutables.that.Assert.that;

public class TestTypes {
	interface Ul<B> {
		B b();
	}

	interface Mj<I> {
		I i();
	}

	interface Hj<T> extends Mj<T> {
		T t();
	}

	static abstract class Bjk<U, V> implements Mj<V> {
		public abstract U u();
		public abstract List<V> v();
	}

	static abstract class Abc extends Bjk<String, Integer> implements Hj<Integer>, Ul<Boolean> {}

	@Test
	public void resolveInHierarchy() throws Exception {
		var variables = Types.mapArgumentsInHierarchy(Abc.class);

		that(Types.resolveArguments(Abc.class.getMethod("b")
			.getGenericReturnType(), variables))
			.same(Boolean.class);

		that(Types.resolveArguments(Abc.class.getMethod("i")
			.getGenericReturnType(), variables))
			.same(Integer.class);

		that(Types.resolveArguments(Abc.class.getMethod("t")
			.getGenericReturnType(), variables))
			.same(Integer.class);

		that(Types.resolveArguments(Abc.class.getMethod("v")
			.getGenericReturnType(), variables))
			.equalTo(Types.newParameterized(List.class, Integer.class));

		that(Types.resolveArguments(Abc.class.getMethod("u")
			.getGenericReturnType(), variables))
			.same(String.class);
	}
}
