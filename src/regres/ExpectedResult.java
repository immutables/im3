package io.immutables.regres;

enum ExpectedResult {
	Update,
	List,
	Optional,
	Single,
	First;

	boolean isOne() {
		return switch (this) {
			case Optional, Single, First -> true;
			default -> false;
		};
	}
}

