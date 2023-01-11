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






import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.annotations.OutputEvent;

/**
 * class IOEventBpmnPart include all event information , - id for the ID of the
 * event, - ClassName for the class name with include the task (IOEvent), -
 * MethodName for method name which annotated by IOEvent, - stepName for the
 * task name, - workflow for the process name, - ioEventType for the event type,
 * - InputEvent for the Input events of the task/part, - outputEvent for the
 * output event of the task/part,
 */
public class IOEventBpmnPart {
	private String id;
	private String apiKey;
	private String ioAppName;
	private String methodQualifiedName;
	private String stepName;
	private String workflow;
	private IOEventType ioEventType;
	private IOEventGatwayInformation ioeventGatway;
	private IOEventExceptionInformation ioeventException;
	private Map<String, String> inputEvent;
	private Map<String, String> outputEvent;
	private int processCount = 0;
	private String methodReturnType;
	private String generalTopic;
	

	

	public IOEventBpmnPart() {
	}

	public IOEventBpmnPart(IOEvent ioEvent,IOFlow ioflow, String id, String apiKey, String ioAppName, String workflow,
			IOEventType ioEventType, String stepName, String methodName, String methodReturnType,String topicPrefix) {
		this.id = id;
		this.apiKey = apiKey;
		this.ioAppName = ioAppName;
		this.workflow = workflow;
		this.ioEventType = ioEventType;
		this.methodQualifiedName = methodName;
		this.methodReturnType=methodReturnType;
		this.stepName = stepName;
		this.ioeventGatway = new IOEventGatwayInformation(ioEvent);
		this.ioeventException = new IOEventExceptionInformation(ioEvent);
		this.inputEvent = this.addInput(ioEvent,ioflow,topicPrefix);
		this.outputEvent = this.addOutput(ioEvent,ioflow,topicPrefix);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getIoAppName() {
		return ioAppName;
	}

	public void setIoAppName(String ioAppName) {
		this.ioAppName = ioAppName;
	}

	public String getMethodQualifiedName() {
		return methodQualifiedName;
	}

	public void setMethodQualifiedName(String methodQualifiedName) {
		this.methodQualifiedName = methodQualifiedName;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public String getWorkflow() {
		return workflow;
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	public IOEventType getIoEventType() {
		return ioEventType;
	}

	public void setIoEventType(IOEventType ioEventType) {
		this.ioEventType = ioEventType;
	}

	public IOEventGatwayInformation getIoeventGatway() {
		return ioeventGatway;
	}

	public String getMethodReturnType() {
		return methodReturnType;
	}

	public void setMethodReturnType(String methodReturnType) {
		this.methodReturnType = methodReturnType;
	}

	public void setIoeventGatway(IOEventGatwayInformation ioeventGatway) {
		this.ioeventGatway = ioeventGatway;
	}

	public Map<String, String> getInputEvent() {
		return inputEvent;
	}

	public void setInputEvent(Map<String, String> inputEvent) {
		this.inputEvent = inputEvent;
	}

	public Map<String, String> getOutputEvent() {
		return outputEvent;
	}

	public void setOutputEvent(Map<String, String> outputEvent) {
		this.outputEvent = outputEvent;
	}

	public int getProcessCount() {
		return processCount;
	}

	public void setProcessCount(int processCount) {
		this.processCount = processCount;
	}
	public String getGeneralTopic() {
		return generalTopic;
	}

	public void setGeneralTopic(String generalTopic) {
		this.generalTopic = generalTopic;
	}
	public Map<String, String> addInput(IOEvent ioEvent,IOFlow ioFlow, String topicPrefix) {
		Map<String, String> result = new HashMap<>();
		for (InputEvent input : ioEvent.input()) {
			if (!StringUtils.isBlank(input.key() + input.value())) {
				if (!StringUtils.isBlank(input.value())) {
					result.put(input.value(),  getEventTopic(input.topic(),ioEvent,ioFlow,topicPrefix));
				} else {
					result.put(input.key(), getEventTopic(input.topic(),ioEvent,ioFlow,topicPrefix));
				}
			}
		}
		for (InputEvent input : ioEvent.gatewayInput().input()) {
			if (!StringUtils.isBlank(input.key() + input.value())) {
				if (!StringUtils.isBlank(input.value())) {
					result.put(input.value(),  getEventTopic(input.topic(),ioEvent,ioFlow,topicPrefix));
				} else {
					result.put(input.key(),  getEventTopic(input.topic(),ioEvent,ioFlow,topicPrefix));
				}
			}
		}
		return result;
	}

	public Map<String, String> addOutput(IOEvent ioEvent,IOFlow ioFlow, String topicPrefix) {
		Map<String, String> result = new HashMap<>();
		if(!StringUtils.isBlank(ioEvent.exception().endEvent().value())) {
			result.put(ioEvent.exception().endEvent().value(), getEventTopic("",ioEvent,ioFlow,topicPrefix));
		}
		boolean isSuffix = false;
		String suffix = "";
		for (OutputEvent output : ioEvent.output()) {
			if (!output.suffix().equals("")) {
				isSuffix = true;
				suffix = output.suffix();
			}
			if (!StringUtils.isBlank(output.key() + output.value())) {
				if (!StringUtils.isBlank(output.value())) {
					result.put(output.value(),getEventTopic(output.topic(),ioEvent,ioFlow,topicPrefix));
				} else {
					result.put(output.key(), getEventTopic(output.topic(),ioEvent,ioFlow,topicPrefix));
				}
			}
		}
		for (OutputEvent output : ioEvent.gatewayOutput().output()) {
			if (!StringUtils.isBlank(output.key() + output.value())) {
				if (!StringUtils.isBlank(output.value())) {
					result.put(output.value(), getEventTopic(output.topic(),ioEvent,ioFlow,topicPrefix));
				} else {
					result.put(output.key(), getEventTopic(output.topic(),ioEvent,ioFlow,topicPrefix));
				}
			}
		}
		if (isSuffix) {
			for (InputEvent input : ioEvent.input()) {
				if (!StringUtils.isBlank(input.key() + input.value())) {
					if (!StringUtils.isBlank(input.value())) {
						result.put(input.value() + suffix, getEventTopic(input.topic(),ioEvent,ioFlow,topicPrefix));
					} else {
						result.put(input.key() + suffix, getEventTopic(input.topic(),ioEvent,ioFlow,topicPrefix));
					}

				}
			}
		}
		if (result.isEmpty()) {
			this.generalTopic=getEventTopic(null,ioEvent,ioFlow,topicPrefix);
		}
		return result;
	}

	private String getEventTopic(String topic, IOEvent ioEvent, IOFlow ioFlow, String topicPrefix) {
		if (!StringUtils.isBlank(topic)) {
			return topicPrefix+topic;
		}
		else if (!StringUtils.isBlank(ioEvent.topic())) {
			return topicPrefix+ioEvent.topic();
		}
		else if (!StringUtils.isBlank(ioFlow.topic())) {
			return topicPrefix+ioFlow.topic();
		}
		return "";
	}

	@Override
	public String toString() {
		return "IOEventBpmnPart [id=" + id + ", apiKey=" + apiKey + ", ioAppName=" + ioAppName
				+ ", methodQualifiedName=" + methodQualifiedName + ", stepName=" + stepName + ", workflow=" + workflow
				+ ", ioEventType=" + ioEventType + ", ioeventGatway=" + ioeventGatway + ", inputEvent=" + inputEvent
				+ ", outputEvent=" + outputEvent + ", processCount=" + processCount + "]";
	}

	public IOEventExceptionInformation getIoeventException() {
		return ioeventException;
	}

	public void setIoeventException(IOEventExceptionInformation ioeventException) {
		this.ioeventException = ioeventException;
	}

}
