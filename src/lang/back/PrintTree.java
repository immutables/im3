package io.immutables.lang.back;

import io.immutables.lang.node.Identifier;
import io.immutables.lang.node.Node;
import io.immutables.lang.node.SourceSpan;
import io.immutables.meta.Null;
import java.lang.reflect.Modifier;
import java.util.List;

public final class PrintTree {
	private Output out;

	public PrintTree(Output out) {
		this.out = out;
	}

	public void print(Node node) {
		try {
			printNode(node);
		} catch (Exception e) {
			throw new RuntimeException("Cannot print node", e);
		}
	}

	private void printNode(Node node) throws Exception {
		var c = node.getClass();
		out.put('<').put(c.getSimpleName()).put('>').ln();
		out.indents++;
		for (var f : c.getFields()) {
			if (Modifier.isStatic(f.getModifiers())) continue;
			if (f.getName().equals("productionIndex")) continue;
			if (f.getName().equals("inParens")) continue;
			out.put(f.getName()).put(": ");
			printValue(f.get(node));
		}
		out.indents--;
	}

	private void printValue(@Null Object o) throws Exception {
    if (o == null) {
      out.put("#null#").ln();
    } else if (o instanceof Node child) {
      printNode(child);
    } else if (o instanceof Identifier identifier) {
      out.put(identifier).ln();
    } else if (o instanceof SourceSpan span) {
      out.put('`').put(span).put('`').ln();
    } else if (o instanceof List<?> list) {
      if (list.isEmpty()) {
        out.put("[]").ln();
      } else {
        out.ln();
        for (var e : list) {
          out.put('-').put(' ');
          printValue(e);
        }
      }
    } else {
      out.put(o).ln();
    }
	}
}
