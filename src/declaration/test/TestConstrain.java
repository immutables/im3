package io.immutables.declaration.test;

import io.immutables.declaration.constrain.Size;

import static io.immutables.declaration.constrain.Constrain.constrain;

public class TestConstrain {
	record X(
		int a,
		int bbbbb,
//		@Min(0) @Max(2)
		int cccc,
		@Size(1) @Regex("1212121")
		String hhhh) {
		X {
			constrain(this)
				.require(a > 0 && a < 3);
		}
	}
}
