package io.immutables.declaration.http;

import io.immutables.meta.Null;
import java.util.Objects;

// TODO checked exception equivalent ?
public abstract class ReturnException extends RuntimeException {
	private Object body = this;
	private int status = 0;

	public void initBody(Object body) {
		if (this.body != this) throw new IllegalStateException("Body is already initialized");
		this.body = Objects.requireNonNull(body);
	}

	public Object getBody() {
		if (body == this) throw new IllegalStateException("Body is not initialized");
		return body;
	}

	public void setStatusCode(int status) {
		if (status <= 0) throw new IllegalArgumentException(
			"Status must be positive int, was: " + status);
		this.status = status;
	}

	public int getStatusCode() {
		int s = status;
		if (s == 0 && (s = inferStatusCode()) == 0) throw new IllegalStateException(
			"No status code is set and cannot be inferred (from @Status annotation)");
		return s;
	}

	private int inferStatusCode() {
		@Null var annotation = getClass().getAnnotation(Status.class);
		return annotation != null ? annotation.value() : 0;
	}
}
