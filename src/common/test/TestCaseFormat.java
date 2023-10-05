package io.immutables.common.test;

import org.junit.Test;

import static io.immutables.common.CaseFormat.*;
import static io.immutables.that.Assert.that;

public class TestCaseFormat {
	@Test public void converts() {
		that(LowerCamel.to(LowerKebob, "abcXyzTuw")).is("abc-xyz-tuw");
		that(UpperCamel.to(LowerSnake, "UPSj")).is("u_p_sj");
		that(UpperSnake.to(UpperCamel, "AR_BU_1")).is("ArBu1");
		that(LowerKebob.to(LowerCamel, "a-bb-ccc-z")).is("aBbCccZ");
		that(LowerKebob.to(UpperSnake, "a-bb-ccc-z")).is("A_BB_CCC_Z");
	}
}
