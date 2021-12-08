package com.grizzlywave.starter.domain;

import java.util.HashMap;
import java.util.Map;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;

public class IOEventBpmnPart {
	private String id;
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

	public IOEventBpmnPart(IOEvent ioEvent, String id, String workflow,IOEventType ioEventType, String stepName, String className,
			String methodName) {
		this.id = id;
		this.workflow = workflow;
		this.ioEventType=ioEventType;
		this.ClassName = className;
		this.MethodName = methodName;
		this.stepName = stepName;
		this.ioeventGatway=new IOEventGatwayInformation(ioEvent);
		this.sourceEvent = this.addSource(ioEvent);
		this.targetEvent = this.addTarget(ioEvent);
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
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

	public Map<String, String> addTarget(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		boolean isSuffix=false;
		String suffix="";
		for (TargetEvent targetEvent : ioEvent.target()) {
			if (!targetEvent.suffix().equals("")) {
				isSuffix = true ;
				suffix=targetEvent.suffix();
			}
			if (!targetEvent.name().equals("")) {
				result.put(targetEvent.name(), targetEvent.topic());
			}
		}
		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!targetEvent.name().equals("")) {
				result.put(targetEvent.name(), targetEvent.topic());
			}
		}
		if (isSuffix) {
			for (SourceEvent sourceEvent : ioEvent.source()) {
				if (!sourceEvent.name().equals("")) {
					result.put(sourceEvent.name()+suffix, sourceEvent.topic());

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
