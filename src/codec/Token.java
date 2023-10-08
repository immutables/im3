package io.immutables.codec;

public enum Token {
	Null,
	Int,
	Long,
	Float,
	True,
	False,
	String,
	Special,
	Struct,
	StructEnd,
	Field,
	Array,
	ArrayEnd,
	End,
	Nope;

	public boolean isScalar() {
		return switch (this) {
			case Null,
					Int,
					Long,
					Float,
					True,
					False,
					String,
					Special -> true;
			default -> false;
		};
	}
}
