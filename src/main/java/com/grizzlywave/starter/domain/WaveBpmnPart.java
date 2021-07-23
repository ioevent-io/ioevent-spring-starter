package com.grizzlywave.starter.domain;

import java.util.UUID;
/** this class have information to be used in the construction of the bpmn*/
public class WaveBpmnPart {

	private UUID id;
	private String eventType;
	private String ClassName;
	private String MethodName;
	private String stepName;
	private String workflow;
	private String sourceEvent;
	private String targetEvent;
	private String source_topic;
	private String target_topic;

	public WaveBpmnPart() {
		super();
	}

	public WaveBpmnPart(UUID uuid, String eventType, String className, String methodName, String stepName,
			String workflow, String targetEvent, String target_topic) {
		this.id = uuid;
		this.eventType = eventType;
		this.ClassName = className;
		this.MethodName = methodName;
		this.stepName = stepName;
		this.workflow = workflow;
		if (eventType.equals("WaveInit")){
		this.targetEvent = targetEvent;
		this.target_topic = target_topic;
		this.source_topic="--";
		this.sourceEvent="--";}
		else {
			this.sourceEvent = targetEvent;
			this.source_topic = target_topic;
			this.target_topic="--";
			this.targetEvent="--";
			
		}
	
	}

	public WaveBpmnPart(UUID id, String eventType, String className, String methodName, String stepName,
			String workflow, String sourceEvent, String targetEvent, String source_topic, String target_topic) {
		this.id = id;
		this.eventType = eventType;
		this.ClassName = className;
		this.MethodName = methodName;
		this.stepName = stepName;
		this.workflow = workflow;
		this.sourceEvent = sourceEvent;
		this.targetEvent = targetEvent;
		this.source_topic = source_topic;
		this.target_topic = target_topic;
	}
	

	

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
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

	public String getSourceEvent() {
		return sourceEvent;
	}

	public void setSourceEvent(String sourceEvent) {
		this.sourceEvent = sourceEvent;
	}

	public String getTargetEvent() {
		return targetEvent;
	}

	public void setTargetEvent(String targetEvent) {
		this.targetEvent = targetEvent;
	}

	public String getSource_topic() {
		return source_topic;
	}

	public void setSource_topic(String source_topic) {
		this.source_topic = source_topic;
	}

	public String getTarget_topic() {
		return target_topic;
	}

	public void setTarget_topic(String target_topic) {
		this.target_topic = target_topic;
	}

}
