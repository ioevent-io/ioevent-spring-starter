/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




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
