package io.immutables.codec;

/**
 * Wrapper type for a content which is supposed to be marshalled
 * as JSON string embedded into another structure/JSON.
 * "Jsons" is a contraction for "JSON-string".
 * But, obviously, in SQL context it can be used to smuggle JSON/JSONB
 * in and out of statements and result-sets.
 * @param <T> type inside
 */
public record Jsons<T>(T content) {}
