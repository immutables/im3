package io.immutables.lang.test;

import io.immutables.lang.node.Term;
import io.immutables.lang.syntax.Grammar;
import io.immutables.lang.syntax.GrammarVm;
import java.util.ArrayList;
import java.util.List;
import static io.immutables.lang.syntax.Grammar.*;

// Sample language that uses that same terms and parsing framework,
// but describes other sample/experimental productions to test things out
public interface BenchLang {
	Factory f = GrammarVm.factory(BenchLang.class, Term.class);

	Production<TypeNode> TypeBinding = f.production(ctx -> new TypeNode());
	Production<List<String>> Array = f.production(ctx -> new ArrayList<>());

	TermCapture<TypeNode> flag = f.termCapture((t, f) -> t.flag = f);

	Alternatives<String> Value = f.alternatives();
	Production<String> Reference = f.production(BenchRun.Stringify::it);
	Production<String> Number = f.production(BenchRun.Stringify::it);
	Ephemeral<Void> Eph = f.ephemeral();

	Part<TypeNode, List<String>> rvalue = f.part((t, a) -> t.val.addAll(a));
	Part<List<String>, String> element = f.part(List::add);

	GrammarVm VM = f.complete(BenchLang::define);

	static void define(Grammar g) {
		g.ignore(Term.Whitespace);

		Term.Info.registerSymbols(g::term);
		g.term(" ", Term.Whitespace);

		g.production(TypeBinding)
				.is(any("\n"), any("//"),
					"type", "{", any("\n"), Term.Name,
					opt(oneOf(flag, "!", "?")), "=", one(rvalue, Array), any("\n"), "}", any("\n"));

		g.production(Array)
				.or("[", "]")
				.or("[", one(element, Value), any(",", one(element, Value)), "]");

		g.production(Value)
			.or(Reference)
			.or(Number);

		g.production(Reference).is(Term.Name);

		g.production(Number).is(Term.IntNumber);
	}

	class TypeNode {
		List<String> val = new ArrayList<>();
		short flag;
	}
}
