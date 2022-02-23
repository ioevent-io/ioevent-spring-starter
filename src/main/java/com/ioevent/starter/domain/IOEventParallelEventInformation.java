package com.ioevent.starter.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
import com.ioevent.starter.handler.IOEventRecordInfo;



/** this class has information about parallel event :
 * - ClassName for the class name which include the @IOEvent annotation,
 * - MethodName for the method name which annotated by  the @IOEvent annotation,
 * - sourceRequired for the source event required to validate the parallel event ,
 * - targetsArrived for the target event arrived,
 * - listenerTopic for topic name which the listener is subscribed,
 * - headers for the header's info sent by events
 * */
public class IOEventParallelEventInformation {

	private String value;
	private List<String> targetsArrived = new ArrayList<>();
	private Map<String, Object> payloadMap=new HashMap<>();
	private String listenerTopic;
	private String method;
	private String className;
	private List<String> sourceRequired;
	private Map<String, Object> headers = new HashMap<>();

	public IOEventParallelEventInformation() {
		super();
	}
	public IOEventParallelEventInformation(String value, List<String> targetsArrived, Map<String, Object> payloadMap,
			String listenerTopic, String method, String className, List<String> sourceRequired,
			Map<String, Object> headers) {
		super();
		this.value = value;
		this.targetsArrived = targetsArrived;
		this.payloadMap = payloadMap;
		this.listenerTopic = listenerTopic;
		this.method = method;
		this.className = className;
		this.sourceRequired = sourceRequired;
		this.headers = headers;
	}
	
	public IOEventParallelEventInformation(ConsumerRecord<String, String> consumerRecord, IOEventRecordInfo ioeventRecordInfo,
			BeanMethodPair pair, List<String> sourceRequired,String appName) {
		super();
		this.value = consumerRecord.value();
		this.payloadMap.put(ioeventRecordInfo.getTargetName(),  consumerRecord.value());
		this.targetsArrived.add(ioeventRecordInfo.getTargetName());
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

	public List<String> getSourceRequired() {
		return sourceRequired;
	}

	public void setSourceRequired(List<String> sourceRequired) {
		this.sourceRequired = sourceRequired;
	}

	public Map<String, Object> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	@Override
	public String toString() {
		return "IOEventParallelEventInformation [value=" + value + ", targetsArrived=" + targetsArrived
				+ ", listenerTopic=" + listenerTopic + ", method=" + method + ", className=" + className
				+ ", sourceRequired=" + sourceRequired + ", headers=" + headers + "]";
	}

}
