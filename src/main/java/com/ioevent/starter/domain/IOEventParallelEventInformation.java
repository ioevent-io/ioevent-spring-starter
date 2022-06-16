/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




package com.ioevent.starter.domain;






import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
import com.ioevent.starter.handler.IOEventRecordInfo;

/**
 * this class has information about parallel event : - ClassName for the class
 * name which include the @IOEvent annotation, - MethodName for the method name
 * which annotated by the @IOEvent annotation, - InputRequired for the Input
 * event required to validate the parallel event , - inputsArrived for the
 * input event arrived, - listenerTopic for topic name which the listener is
 * subscribed, - headers for the header's info sent by events
 */
public class IOEventParallelEventInformation {

	private String value;
	private List<String> inputsArrived = new ArrayList<>();
	private Map<String, Object> payloadMap = new HashMap<>();
	private String listenerTopic;
	private String method;
	private String className;
	private List<String> inputRequired;
	private Map<String, Object> headers = new HashMap<>();

	public IOEventParallelEventInformation() {
		super();
	}

	public IOEventParallelEventInformation(String value, List<String> inputsArrived, Map<String, Object> payloadMap,
			String listenerTopic, String method, String className, List<String> inputRequired,
			Map<String, Object> headers) {
		super();
		this.value = value;
		this.inputsArrived = inputsArrived;
		this.payloadMap = payloadMap;
		this.listenerTopic = listenerTopic;
		this.method = method;
		this.className = className;
		this.inputRequired = inputRequired;
		this.headers = headers;
	}

	public IOEventParallelEventInformation(ConsumerRecord<String, String> consumerRecord,
			IOEventRecordInfo ioeventRecordInfo, BeanMethodPair pair, List<String> inputRequired, String appName) {
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
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<String> getInputsArrived() {
		return inputsArrived;
	}

	public void setInputsArrived(List<String> inputsArrived) {
		this.inputsArrived = inputsArrived;
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

	public List<String> getInputRequired() {
		return inputRequired;
	}

	public void setInputRequired(List<String> inputRequired) {
		this.inputRequired = inputRequired;
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	@Override
	public String toString() {
		return "IOEventParallelEventInformation [value=" + value + ", inputsArrived=" + inputsArrived
				+ ", listenerTopic=" + listenerTopic + ", method=" + method + ", className=" + className
				+ ", inputRequired=" + inputRequired + ", headers=" + headers + "]";
	}

}
