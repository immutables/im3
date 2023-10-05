package io.immutables.regres.test;

import io.immutables.codec.Jsons;

import java.util.List;

public record Bu(int a, String b, Jsons<List<Integer>> c) {}
