package io.immutables.declaration.processor;

record Problem(Severity severity, String message) {
	enum Severity {
		Fatal, Unsupported, Note
	}

	interface Reporter {
		void report(Problem problem);

		default void fatal(String message) {
			report(new Problem(Severity.Fatal, message));
		}
		default void unsupported(String message) {
			report(new Problem(Severity.Unsupported, message));
		}
		default void note(String message) {
			report(new Problem(Severity.Note, message));
		}
	}
}
