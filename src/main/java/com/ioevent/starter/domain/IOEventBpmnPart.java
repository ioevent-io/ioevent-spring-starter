package com.ioevent.starter.domain;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ioevent.starter.annotations.IOEvent;
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
	private String ClassName;
	private String MethodName;
	private String stepName;
	private String workflow;
	private IOEventType ioEventType;
	private IOEventGatwayInformation ioeventGatway;
	private Map<String, String> inputEvent;
	private Map<String, String> outputEvent;
	private int processCount = 0;

	public IOEventBpmnPart() {
	}

	public IOEventBpmnPart(IOEvent ioEvent, String id, String apiKey, String ioAppName, String workflow,
			IOEventType ioEventType, String stepName, String className, String methodName) {
		this.id = id;
		this.apiKey = apiKey;
		this.ioAppName = ioAppName;
		this.workflow = workflow;
		this.ioEventType = ioEventType;
		this.ClassName = className;
		this.MethodName = methodName;
		this.stepName = stepName;
		this.ioeventGatway = new IOEventGatwayInformation(ioEvent);
		this.inputEvent = this.addInput(ioEvent);
		this.outputEvent = this.addOutput(ioEvent);
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

	public String getClassName() {
		return ClassName;
	}

	public void setClassName(String className) {
		ClassName = className;
	}

	public String getMethodName() {
		return MethodName;
	}

	public void setMethodName(String methodName) {
		MethodName = methodName;
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

	public Map<String, String> addInput(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (InputEvent input : ioEvent.input()) {
			if (!StringUtils.isBlank(input.key() + input.value())) {
				if (!StringUtils.isBlank(input.value())) {
					result.put(input.value(), input.topic());
				} else {
					result.put(input.key(), input.topic());
				}

			}
		}
		for (InputEvent input : ioEvent.gatewayInput().input()) {
			if (!StringUtils.isBlank(input.key() + input.value())) {
				if (!StringUtils.isBlank(input.value())) {
					result.put(input.value(), input.topic());
				} else {
					result.put(input.key(), input.topic());
				}
			}
		}
		return result;
	}

	public Map<String, String> addOutput(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		boolean isSuffix = false;
		String suffix = "";
		for (OutputEvent output : ioEvent.output()) {
			if (!output.suffix().equals("")) {
				isSuffix = true;
				suffix = output.suffix();
			}
			if (!StringUtils.isBlank(output.key() + output.value())) {
				if (!StringUtils.isBlank(output.value())) {
					result.put(output.value(), output.topic());
				} else {
					result.put(output.key(), output.topic());
				}
			}
		}
		for (OutputEvent output : ioEvent.gatewayOutput().output()) {
			if (!StringUtils.isBlank(output.key() + output.value())) {
				if (!StringUtils.isBlank(output.value())) {
					result.put(output.value(), output.topic());
				} else {
					result.put(output.key(), output.topic());
				}
			}
		}
		if (isSuffix) {
			for (InputEvent input : ioEvent.input()) {
				if (!StringUtils.isBlank(input.key() + input.value())) {
					if (!StringUtils.isBlank(input.value())) {
						result.put(input.value() + suffix, input.topic());
					} else {
						result.put(input.key() + suffix, input.topic());
					}

				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "IOEventBpmnPart [id=" + id + ", apiKey=" + apiKey + ", ioAppName=" + ioAppName + ", ClassName="
				+ ClassName + ", MethodName=" + MethodName + ", stepName=" + stepName + ", workflow=" + workflow
				+ ", ioEventType=" + ioEventType + ", ioeventGatway=" + ioeventGatway + ", inputEvent=" + inputEvent
				+ ", outputEvent=" + outputEvent + ", processCount=" + processCount + "]";
	}

}
