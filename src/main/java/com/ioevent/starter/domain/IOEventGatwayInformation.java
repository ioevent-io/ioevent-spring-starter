package com.ioevent.starter.domain;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.SourceEvent;
import com.ioevent.starter.annotations.TargetEvent;

/**
 * class for @IOEvent annotation gateway information that will be send within
 * the BPMN Parts to the Admin, it contains information of the type of the
 * gateway also the source events and target events of the gateway.
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
		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!StringUtils.isBlank(targetEvent.key() + targetEvent.value())) {

				if (!StringUtils.isBlank(targetEvent.value())) {
					result.put(targetEvent.value(), targetEvent.topic());
				} else {
					result.put(targetEvent.key(), targetEvent.topic());
				}

			}
		}
		return result;
	}

}
