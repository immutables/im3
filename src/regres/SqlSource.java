package io.immutables.regres;

import io.immutables.common.Source;

record SqlSource(
	String filename,
	CharSequence content,
	Source.Lines lines
) {
	Source.Position get(int position) {
		return lines().get(position);
	}

	Source.Problem problemAt(Source.Range range, String message, String hint) {
		return new Source.Problem(filename(), content(), lines(), range, message, hint);
	}
}
