package io.immutables.lang.syntax;

import io.immutables.lang.Capacity;

public final class Productions<T> {
	private static final int MASK_10 = 0b1111111111;
	private static final int MASK_12 = 0b111111111111;
	private static final int MASK_16 = 0b1111111111111111;

	private static final int SHIFT_0 = 0;
	private static final int SHIFT_1 = 10;
	private static final int SHIFT_2 = 20;
	private static final int SHIFT_3 = 32;
	private static final int SHIFT_4 = 48;

	private long[] words = new long[128];
	private int count;
	int index = -1;

	public int count() {
		return count;
	}

	public int index() {
		return index;
	}

	public long current() {
		if (index == -1) throw new IllegalStateException();
		if (index >= count) return 0;
		return words[index];
	}

	public boolean advance() {
		return !(index == count || ++index == count);
	}

	public void rewind() {
		index = -1;
	}

	public int put(long word) {
		words = Capacity.ensure(words, count, 2); // always padding with at least 1 additional word
		int i = ++index;
		words[i] = word;
		count = i + 1;
		return i;
	}

	void set(int index, long word) {
		words[index] = word;
	}

	public static long encodeProductionPart(long word, int production, int part) {
		assert production > 0
			&& (production & MASK_10) == production && (part & MASK_10) == part;

		return word | production | ((long) part << SHIFT_1);
	}

	/**
	 * Length of the element from its beginning till the end of the last descendant.
	 * Basically, given the offset of production, offset + length will give the offset of
	 * its next sibling or next sibling of one of its parents if not past the end of document.
	 */
	public static long encodeLength(long word, int length) {
		assert (length & MASK_12) == length;
		return word | ((long) length << SHIFT_2);
	}

	public static long encodeTermRange(long word, int begin, int end) {
		assert end >= begin && (begin & MASK_16) == begin && (end & MASK_16) == end;
		return word | ((long) begin << SHIFT_3) | ((long) end << SHIFT_4);
	}

	public static int decodeProduction(long word) {
		return (int) (word & MASK_10);
	}

	public static int decodePart(long word) {
		return (int) ((word >>> SHIFT_1) & MASK_10);
	}

	public static int decodeLength(long word) {
		return (int) ((word >>> SHIFT_2) & MASK_12);
	}

	public static int decodeTermRangeBegin(long word) {
		return (int) ((word >>> SHIFT_3) & MASK_16);
	}

	public static int decodeTermRangeEnd(long word) {
		return (int) ((word >>> SHIFT_4) & MASK_16);
	}
}
