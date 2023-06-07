package io.immutables.build.build;

public record Dependency(String name, boolean isStatic, ProvidingModule module) {}
