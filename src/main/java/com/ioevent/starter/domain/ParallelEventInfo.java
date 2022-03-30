package com.ioevent.starter.domain;

import java.util.List;

/**
 * class which define the parallel event model information :
 *  - id for the ID of the task,
 *  - outputs for the list of outputs of the task,
 */
public class ParallelEventInfo {
	private String id;
	private List<String> outputs;

	public ParallelEventInfo() {
		super();
	}

	public ParallelEventInfo(String id, List<String> outputs) {
		this.id = id;
		this.outputs = outputs;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<String> outputs) {
		this.outputs = outputs;
	}

	@Override
	public String toString() {
		return "CustomEvent [id=" + id + ", outputs=" + outputs + "]";
	}

}
