package io.immutables.declaration.processor;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

class RecordDiscoverer {
	private final TypeElement record;

	RecordDiscoverer(TypeElement record) {
		assert record.getKind() == ElementKind.RECORD;
		this.record = record;
		for (var p : record.getTypeParameters()) {
			var name = p.getSimpleName().toString();
			
		}
	}
}
