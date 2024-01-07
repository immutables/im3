package dev.declaration.processor;

import io.immutables.stencil.Template;
import java.util.ArrayList;
import java.util.List;

abstract class TemplateBase extends Template {

	String version() {
		return "1.0";
	}

	public record Server(String env, String url) {}

	private final List<Server> servers = new ArrayList<>();

	List<Server> servers() {
		return servers;
	}

	String namespaceOf(String name) {
		int dot = name.lastIndexOf('.');
		if (dot >= 0) return name.substring(0, dot);
		return "";
	}

	String toplevelOf(String name) {
		int dot = name.indexOf('.');
		if (dot >= 0) return name.substring(0, dot);
		return "";
	}

	String simplenameOf(String name) {
		int dot = name.lastIndexOf('.');
		if (dot >= 0) return name.substring(dot + 1);

		return name;
	}

	List<String> nsSegmentsOf(String ns) {
		return List.of(ns.split("\\."));
	}
}
