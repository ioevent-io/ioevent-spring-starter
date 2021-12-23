package com.grizzlywave.starter.domain;

import java.util.HashMap;
import java.util.Map;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;

/**
 * class for @IOEvent annotation gateway information that will be sent within
 * the BPMN Parts to the Admin, it contains information of the type of the gateway
 * also the source events and target events of the gateway.
 **/
public class IOEventGatwayInformation {

	private Boolean exclusiveSource;
	private Boolean parallelSource;
	private Boolean exclusiveTarget;
	private Boolean parallelTarget;
	private Map<String, String> sourceEvent;
	private Map<String, String> targetEvent;

	public IOEventGatwayInformation() {
	}

	public IOEventGatwayInformation(Boolean exclusiveSource, Boolean parallelSource, Boolean exclusiveTarget,
			Boolean parallelTarget, Map<String, String> sourceEvent, Map<String, String> targetEvent) {
		this.exclusiveSource = exclusiveSource;
		this.parallelSource = parallelSource;
		this.exclusiveTarget = exclusiveTarget;
		this.parallelTarget = parallelTarget;
		this.sourceEvent = sourceEvent;
		this.targetEvent = targetEvent;
	}

	public IOEventGatwayInformation(IOEvent ioEvent) {

		this.exclusiveSource = ioEvent.gatewaySource().exclusive();
		this.parallelSource = ioEvent.gatewaySource().parallel();
		this.exclusiveTarget = ioEvent.gatewayTarget().exclusive();
		this.parallelTarget = ioEvent.gatewayTarget().parallel();
		this.sourceEvent = this.addSource(ioEvent);
		this.targetEvent = this.addTarget(ioEvent);
	}

	public Boolean getExclusiveSource() {
		return exclusiveSource;
	}

	public void setExclusiveSource(Boolean exclusiveSource) {
		this.exclusiveSource = exclusiveSource;
	}

	public Boolean getParallelSource() {
		return parallelSource;
	}

	public void setParallelSource(Boolean parallelSource) {
		this.parallelSource = parallelSource;
	}

	public Boolean getExclusiveTarget() {
		return exclusiveTarget;
	}

	public void setExclusiveTarget(Boolean exclusiveTarget) {
		this.exclusiveTarget = exclusiveTarget;
	}

	public Boolean getParallelTarget() {
		return parallelTarget;
	}

	public void setParallelTarget(Boolean parallelTarget) {
		this.parallelTarget = parallelTarget;
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

	public Map<String, String> addSource(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!sourceEvent.name().equals("")) {
				result.put(sourceEvent.name(), sourceEvent.topic());
			}
		}
		return result;
	}

	public Map<String, String> addTarget(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!targetEvent.name().equals("")) {
				result.put(targetEvent.name(), targetEvent.topic());
			}
		}
		return result;
	}

}
