package dev.declaration.processor;

import dev.declaration.processor.Declaration.Datatype;
import dev.declaration.processor.Declaration.Sealed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;

/** Unique pieces to disambiguate variants */
sealed interface VariantFeature {
	/**
	 * Unique shapes of a variant. These are based on most
	 * common-denominator JSON encoding.
	 * More elaborate encoding may allow variants to differ
	 * in move variety of basic types, but this should not be
	 * a typical usage
	 */
	enum Shape implements VariantFeature {
		Boolean, Number, String, Record, Product, Unspecified
	}

	/** Presence of a field. Value is ignored here */
	record HasField(String name) implements VariantFeature {}

	/** Tag field matches, both name and value. */
	// TODO add support for tags when new codecs released
	record TagFieldMatches(String name, String value) implements VariantFeature {}

	// This routine doesn't look for edge cases like unvalidated (by processor)
	// variants. Actually it can be turned into such validator.
	// The logic here should be coherent with/mostly(or better, fully) match
	// how codecs handle polymorphic parsing.
	static Map<Datatype, List<VariantFeature>> uniquesFrom(Sealed variant) {
		var byFeature = listAllFeatures(variant).stream()
			.collect(groupingBy(Entry::getValue));

		// it doesn't matter if here we use List or Set as values,
		// there will be no duplicates
		var uniqueByCase = new HashMap<Datatype, List<VariantFeature>>();

		for (var overlapping : byFeature.values()) {
			// really non-overlapping unique features,
			// attributing it to a case
			if (overlapping.size() == 1) {
				var entry = overlapping.get(0);
				var datatype = entry.getKey();
				var feature = entry.getValue();

				uniqueByCase.computeIfAbsent(datatype,
						k -> new ArrayList<>()).add(feature);
			}
		}
		return uniqueByCase;
	}

	private static List<Entry<Datatype, VariantFeature>> listAllFeatures(Sealed variant) {
		var entries = new ArrayList<Entry<Datatype, VariantFeature>>();

		for (var c : variant.cases()) {
			if (c instanceof Declaration.Inline il
				&& il.component().type() instanceof Type.Primitive primitive) {
				var shape = switch (primitive) {
					case String -> Shape.String;
					case Boolean -> Shape.Boolean;
					case Integer, Long, Float -> Shape.Number;
					default -> Shape.Unspecified;
				};
				entries.add(entry(c, shape));
			} else if (c instanceof Declaration.Inline il
				&& il.component().type() instanceof Type.Extended) {
				entries.add(entry(c, Shape.String));
			} else if (c instanceof Declaration.Enum) {
				entries.add(entry(c, Shape.String));
			} else if (c instanceof Declaration.Record rc) {
				entries.add(entry(c, Shape.Record));
				for (var component : rc.components()) {
					entries.add(entry(c,
						new HasField(component.name())));
				}
			} else { // TODO expand if applicable
				entries.add(entry(c, Shape.Unspecified));
			}
		}

		return entries;
	}
}
