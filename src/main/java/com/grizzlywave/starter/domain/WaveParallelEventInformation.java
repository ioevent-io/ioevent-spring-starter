package com.grizzlywave.starter.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.grizzlywave.starter.configuration.postprocessor.BeanMethodPair;
import com.grizzlywave.starter.handler.WaveRecordInfo;

public class WaveParallelEventInformation {

	private String value;
	private List<String> targetsArrived = new ArrayList<String>();
	private String listenerTopic;
	private String method;
	private String className;
	private List<String> sourceRequired;
	private Map<String, String> headers = new HashMap<String, String>();

	public WaveParallelEventInformation() {
		super();
	}

	public WaveParallelEventInformation(String value, List<String> targetsArrived, String listenerTopic, String method,
			String className, List<String> sourceRequired, Map<String, String> headers) {
		super();
		this.value = value;
		this.targetsArrived = targetsArrived;
		this.listenerTopic = listenerTopic;
		this.method = method;
		this.className = className;
		this.sourceRequired = sourceRequired;
		this.headers = headers;
	}

	public WaveParallelEventInformation(ConsumerRecord<String, String> consumerRecord, WaveRecordInfo waveRecordInfo,
			BeanMethodPair pair, List<String> sourceRequired,String appName) {
		super();
		this.value = consumerRecord.value();
		this.targetsArrived.add(waveRecordInfo.getTargetName());
		this.listenerTopic = consumerRecord.topic();
		this.method = pair.getMethod().getName();
		this.className = pair.getBean().getClass().getName();
		this.sourceRequired = sourceRequired;
		headers.put("AppName", appName);
		consumerRecord.headers().forEach(header -> this.headers.put(header.key(), new String(header.value())));
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<String> getTargetsArrived() {
		return targetsArrived;
	}

	public void setTargetsArrived(List<String> targetsArrived) {
		this.targetsArrived = targetsArrived;
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

	public List<String> getSourceRequired() {
		return sourceRequired;
	}

	public void setSourceRequired(List<String> sourceRequired) {
		this.sourceRequired = sourceRequired;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	@Override
	public String toString() {
		return "WaveParallelEventInformation [value=" + value + ", targetsArrived=" + targetsArrived
				+ ", listenerTopic=" + listenerTopic + ", method=" + method + ", className=" + className
				+ ", sourceRequired=" + sourceRequired + ", headers=" + headers + "]";
	}

}
