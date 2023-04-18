package io.immutables.regres;

/**
 * Wrapper type for a content which is supposed to be marshalled
 * as JSON into/from a string. "Jsons" is a contraction for "JSON-string".
 * But, obviously, in SQL context it's used to smuggle JSONB in and out of
 * statements and result-sets.
 * @param <T> type inside
 */
public record Jsons<T>(T content) {}
