package com.ioevent.starter.domain;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.annotations.OutputEvent;

/**
 * class for @IOEvent annotation gateway information that will be send within
 * the BPMN Parts to the Admin, it contains information of the type of the
 * gateway also the input events and output events of the gateway.
 **/
public class IOEventGatwayInformation {

	private Boolean exclusiveInput;
	private Boolean parallelInput;
	private Boolean exclusiveOutput;
	private Boolean parallelOutput;
	private Map<String, String> inputEvent;
	private Map<String, String> outputEvent;

	public IOEventGatwayInformation() {
	}

	public IOEventGatwayInformation(Boolean exclusiveInput, Boolean parallelInput, Boolean exclusiveOutput,
			Boolean parallelOutput, Map<String, String> inputEvent, Map<String, String> outputEvent) {
		this.exclusiveInput = exclusiveInput;
		this.parallelInput = parallelInput;
		this.exclusiveOutput = exclusiveOutput;
		this.parallelOutput = parallelOutput;
		this.inputEvent = inputEvent;
		this.outputEvent = outputEvent;
	}

	public IOEventGatwayInformation(IOEvent ioEvent) {

		this.exclusiveInput = ioEvent.gatewayInput().exclusive();
		this.parallelInput = ioEvent.gatewayInput().parallel();
		this.exclusiveOutput = ioEvent.gatewayOutput().exclusive();
		this.parallelOutput = ioEvent.gatewayOutput().parallel();
		this.inputEvent = this.addInput(ioEvent);
		this.outputEvent = this.addOutput(ioEvent);
	}

	public Boolean getExclusiveInput() {
		return exclusiveInput;
	}

	public void setExclusiveInput(Boolean exclusiveInput) {
		this.exclusiveInput = exclusiveInput;
	}

	public Boolean getParallelInput() {
		return parallelInput;
	}

	public void setParallelInput(Boolean parallelInput) {
		this.parallelInput = parallelInput;
	}

	public Boolean getExclusiveOutput() {
		return exclusiveOutput;
	}

	public void setExclusiveOutput(Boolean exclusiveOutput) {
		this.exclusiveOutput = exclusiveOutput;
	}

	public Boolean getParallelOutput() {
		return parallelOutput;
	}

	public void setParallelOutput(Boolean parallelOutput) {
		this.parallelOutput = parallelOutput;
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

	public Map<String, String> addInput(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (InputEvent inputEvent : ioEvent.gatewayInput().input()) {
			if (!StringUtils.isBlank(inputEvent.key() + inputEvent.value())) {

				if (!StringUtils.isBlank(inputEvent.value())) {
					result.put(inputEvent.value(), inputEvent.topic());
				} else {
					result.put(inputEvent.key(), inputEvent.topic());
				}

			}

		}

		return result;
	}

	public Map<String, String> addOutput(IOEvent ioEvent) {
		Map<String, String> result = new HashMap<String, String>();
		for (OutputEvent outputEvent : ioEvent.gatewayOutput().output()) {
			if (!StringUtils.isBlank(outputEvent.key() + outputEvent.value())) {

				if (!StringUtils.isBlank(outputEvent.value())) {
					result.put(outputEvent.value(), outputEvent.topic());
				} else {
					result.put(outputEvent.key(), outputEvent.topic());
				}

			}
		}
		return result;
	}

}
