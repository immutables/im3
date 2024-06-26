package io.immutables.lang.test;

import io.immutables.lang.typeold2.Type;
import io.immutables.lang.typeold2.Types;
import java.util.Map;
import org.junit.Test;
import static io.immutables.lang.test.FixtureTypes.*;
import static io.immutables.that.Assert.that;

public class TestTypes {
	@Test
	public void substitution() {
		var A = Type.Parameter.introduce(p.id("A"), 0);
		var B = Type.Parameter.introduce(p.id("B"), 1);

		var A_ = Type.Variable.allocate(A.name());
		var B_ = Type.Variable.allocate(B.name());

		var subs = Map.of(A, A_, B, B_);
		var before = Type.Product.of(A, B);
		var after = before.transform(Types.substitution(subs));

		that(after).equalTo(Type.Product.of(A_, B_));
	}

	@Test
	public void signature() {
		System.out.println(productOf(bool, i32));
		System.out.println(Aa_T);
		System.out.println(Bb_W_Y);
	}
}
