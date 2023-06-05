package io.immutables.stencil.template;

import io.immutables.stencil.Current;
import javax.annotation.processing.ProcessingEnvironment;

public class ProcessingCurrent extends Current {
	final ProcessingEnvironment processing;

	public ProcessingCurrent(ProcessingEnvironment processing) {
		this.processing = processing;
	}
}
