package com.ioevent.starter.domain;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.SourceEvent;
import com.ioevent.starter.annotations.TargetEvent;

/**
 * class IOEventBpmnPart include all event information , - id for the ID of the
 * event, - ClassName for the class name with include the task (IOEvent), -
 * MethodName for method name which annotated by IOEvent, - stepName for the
 * task name, - workflow for the process name, - ioEventType for the event type,
 * - sourceEvent for the source events of the task/part, - targetEvent for the
 * target event of the task/part,
 */
public class IOEventBpmnPart {
	private String id;
	private String apiKey;
	private String ClassName;
	private String MethodName;
	private String stepName;
	private String workflow;
	private IOEventType ioEventType;
	private IOEventGatwayInformation ioeventGatway;
	private Map<String, String> sourceEvent;
	private Map<String, String> targetEvent;
	private int processCount = 0;

	public IOEventBpmnPart() {
	}

	public IOEventBpmnPart(IOEvent ioEvent, String id, String apiKey, String workflow, IOEventType ioEventType,
			String stepName, String className, String methodName) {
		this.id = id;
		this.apiKey = apiKey;
		this.workflow = workflow;
		this.ioEventType = ioEventType;
		this.ClassName = className;
		this.MethodName = methodName;
		this.stepName = stepName;
		this.ioeventGatway = new IOEventGatwayInformation(ioEvent);
		this.sourceEvent = this.addSource(ioEvent);
		this.targetEvent = this.addTarget(ioEvent);
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

	public Map<String, String> getSourceEvent() {
		return sourceEvent;
	}

	public void setSourceEvent(Map<String, String> sourceEvent) {
		this.sourceEvent = sourceEvent;
	}

	public Map<String, String> getTargetEvent() {
		return targetEvent;
	}

	public void setTargetEvent(Map<String, String> targetEvent) {
		this.targetEvent = targetEvent;
	}

	public int getProcessCount() {
		return processCount;
	}

	public void setProcessCount(int processCount) {
		this.processCount = processCount;
	}

	public Map<String, String> addSource(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
				if (!StringUtils.isBlank(sourceEvent.value())) {
					result.put(sourceEvent.value(), sourceEvent.topic());
				} else {
					result.put(sourceEvent.key(), sourceEvent.topic());
				}

			}
		}
		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
				if (!StringUtils.isBlank(sourceEvent.value())) {
					result.put(sourceEvent.value(), sourceEvent.topic());
				} else {
					result.put(sourceEvent.key(), sourceEvent.topic());
				}
			}
		}
		return result;
	}

	public Map<String, String> addTarget(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		boolean isSuffix = false;
		String suffix = "";
		for (TargetEvent targetEvent : ioEvent.target()) {
			if (!targetEvent.suffix().equals("")) {
				isSuffix = true;
				suffix = targetEvent.suffix();
			}
			if (!StringUtils.isBlank(targetEvent.key() + targetEvent.value())) {
				if (!StringUtils.isBlank(targetEvent.value())) {
					result.put(targetEvent.value(), targetEvent.topic());
				}else {
					result.put(targetEvent.key(), targetEvent.topic());
				}
			}
		}
		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!StringUtils.isBlank(targetEvent.key() + targetEvent.value())) {
				if (!StringUtils.isBlank(targetEvent.value())) {
					result.put(targetEvent.value(), targetEvent.topic());
				}else {
					result.put(targetEvent.key(), targetEvent.topic());
				}
			}
		}
		if (isSuffix) {
			for (SourceEvent sourceEvent : ioEvent.source()) {
				if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
					if (!StringUtils.isBlank(sourceEvent.value())) {
						result.put(sourceEvent.value() + suffix, sourceEvent.topic());
					} else {
						result.put(sourceEvent.key() + suffix, sourceEvent.topic());
					}

				}
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "IOEventBpmnPart [id=" + id + ", ClassName=" + ClassName + ", MethodName=" + MethodName + ", stepName="
				+ stepName + ", workflow=" + workflow + ", ioEventType=" + ioEventType + ", sourceEvent=" + sourceEvent
				+ ", targetEvent=" + targetEvent + "]";
	}

}
