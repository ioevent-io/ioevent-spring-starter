package com.grizzlywave.starter.model;

public class WaveResponseHeader {
	private String workflow;
	private String sourceEvent;
	private String targetEvent;
	private String eventType;

	public WaveResponseHeader() {
		super();
	}

	public WaveResponseHeader(String workflow, String sourceEvent, String targetEvent, String eventType) {
		this.workflow = workflow;
		this.sourceEvent = sourceEvent;
		this.targetEvent = targetEvent;
		this.eventType = eventType;
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

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	@Override
	public String toString() {
		return "WaveResponseHeader [workflow=" + workflow + ", sourceEvent=" + sourceEvent + ", targetEvent="
				+ targetEvent + ", eventType=" + eventType + "]";
	}
	
	
}
