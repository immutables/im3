package io.immutables.stencil;

import io.immutables.meta.Null;
import java.util.Arrays;
import java.util.stream.Stream;

@Deprecated
public class Comments extends Stencil.Raw {
	public Content lines(String prefix) {
		var out = out();
		return block("", prefix, "");
	}

	public Content block(String before, String each, String after) {
		var out = out();
		return new Content() {
			public void content(Runnable content) {
				out.ifln();

				if (!before.isEmpty()) {
					out.put(before).ln();
				}

				// save previous indents
				int wasIndents = out.indents;
				@Null String wasIndentPrefix = out.indentPrefix;

				// compute new indent prefix
				StringBuilder sb = out.currentIndent();
				sb.append(each);
				out.indentPrefix = sb.toString();
				out.indents = 0;
				try {
					content.run();
				} finally {
					// restore to previous indent
					out.indents = wasIndents;
					out.indentPrefix = wasIndentPrefix;
				}

				if (!after.isEmpty()) {
					out.ifln().put(after).ln();
				}
			}

			public void content(Iterable<? extends CharSequence> lines) {
				content(() -> {
					for (var l : lines) {
						out.put(l).ln();
					}
				});
			}
		};
	}

	public interface Content {
		void content(Runnable content);
		void content(Iterable<? extends CharSequence> lines);

		default void content(String... lines) {
			content(Arrays.asList(lines));
		}

		default void content(CharSequence content) {
			content(content.toString().split("\\n"));
		}

		default void content(Stream<? extends CharSequence> lines) {
			content(lines.toList());
		}
	}
}
