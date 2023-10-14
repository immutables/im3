package io.immutables.codec.record;

/**
 * Carries information about case tag. Case tag is an additional field+value
 * that is added (Currently only string values) to a JSON (or alike) representation
 * or a record implementing sealed interface, representing disjoint union of cases.
 * Tags are added to, either add expressiveness to a case or, more often,
 * to resolve any ambiguity arising from simply comparing keys and picking first
 * codec which match.
 * @param field field name
 * @param value value, which can be used as discriminator (if same field name)
 */
public record CaseTag(String field, String value) {}
