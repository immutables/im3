package io.immutables.lang.syntax;

import io.immutables.lang.Capacity;

public final class Terms {
	private int[] tokens = new int[16];
	// limit is not length, it's last element index
	private int lastIndex = -1;
	// shared package access to internal index
	// can be used for efficient reset during parsing
	// index()/seek() can do the same but with a lot of
	// boundary checks
	int index = -1;

	public Terms() {
		this(0);
	}

	public Terms(int sourcePositionBefore) {
		tokens[0] = sourcePositionBefore;
	}

	public int count() {
		return (lastIndex + 1) >> 1;
	}

	public short seek(int index) {
		if (index == -1) {
			this.index = -1; // same as flip
			return SOI;
		}
		int i = (index << 1) + 1;
		if (i < 0 || i > lastIndex + STEP) {
			throw new IndexOutOfBoundsException(index);
		}
		this.index = i;
		return i > lastIndex ? EOI : (short) tokens[i];
	}

	public int index() {
		return (index - 1) >> 1;
	}

	public short current() {
		return index < 0 ? SOI
			: (index > lastIndex ? EOI
			: (short) tokens[index]);
	}

	public int sourcePositionBefore() {
		assert index > 0 && index <= lastIndex;
		return tokens[index - 1];
	}

	public int sourcePositionAfter() {
		assert index > 0 && index <= lastIndex;
		return tokens[index + 1];
	}

	public int sourcePositionBefore(int index) {
		int i = (index << 1) + 1;
		return tokens[i - 1];
	}

	public int sourcePositionAfter(int index) {
		int i = (index << 1) + 1;
		return tokens[i + 1];
	}

	public boolean extend(short term, int sourcePositionAfter) {
		if (index > 0 && tokens[index] == term) {
			tokens[index + 1] = sourcePositionAfter;
			return true;
		}
		return put(term, sourcePositionAfter);
	}

	public boolean put(short term, int sourcePositionAfter) {
		tokens = Capacity.ensure(tokens, lastIndex, STEP_INCREMENT);
		if (lastIndex == index) lastIndex += STEP;
		index += STEP;

		tokens[index] = term;
		tokens[index + 1] = sourcePositionAfter;
		return true;
	}

	public void rewind() {
		index = -1;
	}

	public short next() {
		if (index > lastIndex) return EOI;
		index += STEP;
		return index > lastIndex ? EOI : (short) tokens[index];
	}

	public short at(int index) {
		int i = (index << 1) + 1;
		return (short) tokens[i];
	}

	static final short EOI = -1;
	static final short SOI = -2;

	private static final int STEP = 2;
	private static final int STEP_INCREMENT = STEP + STEP;
}
