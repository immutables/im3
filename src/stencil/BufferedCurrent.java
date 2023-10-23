package io.immutables.stencil;

public class BufferedCurrent extends Current {
	public StringBuilder content() {
		return out.raw;
	}
}
