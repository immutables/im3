package dev.declaration.http;

import io.immutables.meta.Null;
import io.immutables.meta.SkippableReturn;
import static java.util.Objects.requireNonNull;

// TODO checked exception equivalent ?
// TODO (setStatusCode, setStatusText) vs setStatusText
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
		setStatusCode(code);
		setStatusText(text);
		return this;
	}

	public boolean hasBody() {
		return body != this;
	}

	public Object getBody() {
		if (body == this) throw new IllegalStateException("Body is not initialized");
		return body;
	}

	public void setStatusCode(int statusCode) {
		if (statusCode <= 0) throw new IllegalArgumentException(
			"Status must be positive int, was: " + statusCode);
		this.statusCode = statusCode;
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

	public void setStatusText(String statusText) {
		this.statusText = requireNonNull(statusText);
	}

	public String getStatusText() {
		return statusText;
	}

	@Override public String getMessage() {
		return getStatusCode() + " " + getStatusText();
	}
}
