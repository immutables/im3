package dev.declaration.http;

import io.immutables.meta.Null;
import io.immutables.meta.SkippableReturn;
import static java.util.Objects.requireNonNull;

public abstract class ReturnException extends RuntimeException {
	private Object body = this;
	private int statusCode = 0;
	private String statusText = this.getClass().getSimpleName();

	@SkippableReturn
	public ReturnException initBody(Object body) {
		if (this.body != this) throw new IllegalStateException("Body is already initialized");
		this.body = requireNonNull(body);
		return this;
	}

	@SkippableReturn
	public ReturnException initStatus(int code, String text) {
		if (statusCode != 0) throw new IllegalStateException("Status code already initialized");
		if (code <= 0) throw new IllegalArgumentException(
			"Status must be positive int, was: " + code);
		this.statusCode = code;
		this.statusText = requireNonNull(text);
		return this;
	}

	public boolean hasBody() {
		return body != this;
	}

	Object getBody() {
		if (body == this) throw new IllegalStateException("Body is not initialized");
		return body;
	}

	public int getStatusCode() {
		int s = statusCode;
		if (s == 0 && (s = inferStatusCode()) == 0) throw new IllegalStateException(
			"No status code is set and cannot be inferred (from @Status annotation)");
		return s;
	}

	private int inferStatusCode() {
		@Null var annotation = getClass().getAnnotation(Status.class);
		return annotation != null ? annotation.value() : 0;
	}

	public String getStatusText() {
		return statusText;
	}

	@Override public String getMessage() {
		return getStatusCode() + " " + getStatusText();
	}
}
