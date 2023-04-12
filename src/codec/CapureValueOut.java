package io.immutables.codec;

import java.io.IOException;

public class CapureValueOut extends Out {
	public String stringValue;
	public int intValue;
	public boolean booleanValue;
	public double doubleValue;

	public void putNull() throws IOException {

	}

	public void putInt(int i) throws IOException {
		this.intValue = i;
	}

	public void putLong(long l) throws IOException {

	}

	public void putDouble(double d) throws IOException {
		this.doubleValue = d;
	}

	public void putBoolean(boolean b) throws IOException {
		this.booleanValue = b;
	}

	public void putString(String s) throws IOException {
		this.stringValue = s;
	}

	public void putChars(char[] chars, int offset, int length) throws IOException {

	}

	public void putString(NameIndex names, int index) throws IOException {

	}

	public void beginArray() throws IOException {

	}

	public void endArray() throws IOException {

	}

	public void beginStruct(NameIndex names) throws IOException {

	}

	public void putField(int index) throws IOException {

	}

	public void putField(String name) throws IOException {

	}

	public void endStruct() throws IOException {

	}

	public NameIndex index(String... known) {
		return NameIndex.unknown();
	}
}
