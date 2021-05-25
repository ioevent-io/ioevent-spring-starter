package com.grizzlywave.starter.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;

public class IOEventBpmnPart {
	private UUID id;
	private String ClassName;
	private String MethodName;
	private String stepName;
	private String workflow;
	private Map<String, String> sourceEvent;
	private Map<String, String>  targetEvent;

	
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
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
	public Map<String, String>  getSourceEvent() {
		return sourceEvent;
	}
	public void setSourceEvent(Map<String, String>  sourceEvent) {
		this.sourceEvent = sourceEvent;
	}
	public Map<String, String>  getTargetEvent() {
		return targetEvent;
	}
	public void setTargetEvent(Map<String, String>  targetEvent) {
		this.targetEvent = targetEvent;
	}
	

	public IOEventBpmnPart() {
	}
	public IOEventBpmnPart(IOEvent ioEvent,UUID id,String workflow, String stepName, String className, String methodName) {
		this.id = id;
		this.workflow=workflow;
		this.ClassName = className;
		this.MethodName = methodName;
		this.stepName = stepName;
		this.sourceEvent = this.getsource(ioEvent);
		this.targetEvent = this.getTarget(ioEvent);
	}
	public Map<String, String> getsource(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!sourceEvent.name().equals("")) {
				result.put(sourceEvent.name(), sourceEvent.topic());

			}
		}
		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!sourceEvent.name().equals("")) {
				result.put(sourceEvent.name(), sourceEvent.topic());

			}
		}
		return result;
	}
	public Map<String, String> getTarget(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (TargetEvent targetEvent : ioEvent.target()) {
		if (!targetEvent.name().equals("")) {
			result.put(targetEvent.name(), targetEvent.topic());
		}
		}
		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!targetEvent.name().equals("")) {
				result.put(targetEvent.name(), targetEvent.topic());
			}
		}
		return result;
	}
}
