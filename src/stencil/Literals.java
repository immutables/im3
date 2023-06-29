package io.immutables.stencil;

public class Literals {
	private Literals() {}

	public static CharSequence string(CharSequence value) {
		var b = new StringBuilder();
		int len = value.length();
		b.append('"');
		for (int i = 0; i < len; i++) {
			escapeInto(b, value.charAt(i));
		}
		return b.append('"').toString();
	}

	private static void escapeInto(StringBuilder b, char c) {
		switch (c) {
			case '\"' -> b.append("\\\"");
			case '\'' -> b.append("\\'");
			case '\\' -> b.append("\\\\");
			case '\b' -> b.append("\\b");
			case '\f' -> b.append("\\f");
			case '\n' -> b.append("\\n");
			case '\r' -> b.append("\\r");
			case '\t' -> b.append("\\t");
			case '\0' -> b.append("\\0");
			default -> {
				if (c < ' '/* 0x20 */ || c > '~'/* 0x7E */) {
					var code = new char[]{'\\', 'u', 0, 0, 0, 0};
					code[5] = hex[c & 0xF];
					c >>>= 4;
					code[4] = hex[c & 0xF];
					c >>>= 4;
					code[3] = hex[c & 0xF];
					c >>>= 4;
					code[2] = hex[c & 0xF];
					b.append(code);
				} else {
					b.append(c);
				}
			}
		}
	}

	private static final char[] hex = "0123456789abcdef".toCharArray();
}
