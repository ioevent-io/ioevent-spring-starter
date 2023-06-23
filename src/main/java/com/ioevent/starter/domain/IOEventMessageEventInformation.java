package com.ioevent.starter.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
import com.ioevent.starter.handler.IOEventRecordInfo;

public class IOEventMessageEventInformation {

	private String value;
	private Map<String, Object> payloadMap = new HashMap<>();
	private String listenerTopic;
	private String method;
	private String className;
	private Map<String, Object> headers = new HashMap<>();
	private List<String> inputRequired;
	private List<String> inputsArrived = new ArrayList<>();
	private String messageEventRequired ;
	private String messageEventArrived ;
	
	
	
	
	public IOEventMessageEventInformation(ConsumerRecord<String, String> consumerRecord,
			IOEventRecordInfo ioeventRecordInfo, BeanMethodPair pair, List<String> inputRequired, String appName,
			String messageKey) {
		super();
		this.value = consumerRecord.value();
		this.payloadMap.put(ioeventRecordInfo.getOutputConsumedName(), consumerRecord.value());
		this.inputsArrived.add(ioeventRecordInfo.getOutputConsumedName());
		this.listenerTopic = consumerRecord.topic();
		this.method = pair.getMethod().getName();
		this.className = pair.getBean().getClass().getName();
		this.inputRequired = inputRequired;
		headers.put("AppName", appName);
		consumerRecord.headers().forEach(header -> this.headers.put(header.key(), new String(header.value())));
		this.messageEventRequired = pair.getIoEvent().message().key();
		this.messageEventArrived = ioeventRecordInfo.getMessageKey();
	}
	public IOEventMessageEventInformation() {
		super();
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public Map<String, Object> getPayloadMap() {
		return payloadMap;
	}
	public void setPayloadMap(Map<String, Object> payloadMap) {
		this.payloadMap = payloadMap;
	}
	public String getListenerTopic() {
		return listenerTopic;
	}
	public void setListenerTopic(String listenerTopic) {
		this.listenerTopic = listenerTopic;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public Map<String, Object> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}
	public List<String> getInputRequired() {
		return inputRequired;
	}
	public void setInputRequired(List<String> inputRequired) {
		this.inputRequired = inputRequired;
	}
	public List<String> getInputsArrived() {
		return inputsArrived;
	}
	public void setInputsArrived(List<String> inputsArrived) {
		this.inputsArrived = inputsArrived;
	}
	public String getMessageEventRequired() {
		return messageEventRequired;
	}
	public void setMessageEventRequired(String messageEventRequired) {
		this.messageEventRequired = messageEventRequired;
	}
	public String getMessageEventArrived() {
		return messageEventArrived;
	}
	public void setMessageEventArrived(String messageEventArrived) {
		this.messageEventArrived = messageEventArrived;
	}
	@Override
	public String toString() {
		return "IOEventMessageEventInformation [value=" + value + ", payloadMap=" + payloadMap + ", listenerTopic="
				+ listenerTopic + ", method=" + method + ", className=" + className + ", headers=" + headers
				+ ", inputRequired=" + inputRequired + ", inputsArrived=" + inputsArrived + ", messageEventRequired="
				+ messageEventRequired + ", messageEventArrived=" + messageEventArrived + "]";
	}
	
	
	
}
