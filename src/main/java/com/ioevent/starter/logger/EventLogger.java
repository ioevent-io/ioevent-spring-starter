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




package com.ioevent.starter.logger;






import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.kafka.support.KafkaNull;

import com.fasterxml.jackson.annotation.JsonInclude;

/**class event logger used to log ioevent Annotation aspect */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventLogger {
	private String correlationId;
	private String ioflow;
	private String stepName;
	private String inputEvent;
	private String outputEvent;
	private String eventType;
	private Object payload;
	private String startTime;
	private String endTime;
	private Long duration;
	private String errorType;
	public EventLogger() {
		super();
	}

	public EventLogger(String correlationId, String ioflow, String stepName, String inputEvent, String outputEvent,
			String eventType, Object payload) {
		this.correlationId = correlationId;
		this.ioflow = ioflow;
		this.stepName = stepName;
		this.inputEvent = inputEvent;
		this.outputEvent = outputEvent;
		this.eventType = eventType;
		this.payload = payload;
	} 

	public EventLogger(String correlationId, String ioflow, String stepName, String inputEvent, String outputEvent,
			String eventType, Object payload, String startTime, String endTime, Long duration) {
		this.correlationId = correlationId;
		this.ioflow = ioflow;
		this.stepName = stepName;
		this.inputEvent = inputEvent;
		this.outputEvent = outputEvent;
		this.eventType = eventType;
		this.payload = payload;
		this.startTime = startTime;
		this.endTime = endTime;
		this.duration = duration;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public String getIoflow() {
		return ioflow;
	}

	public void setIoflow(String ioflow) {
		this.ioflow = ioflow;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public String getInputEvent() {
		return inputEvent;
	}

	public void setInputEvent(String inputEvent) {
		this.inputEvent = inputEvent;
	}

	public String getOutputEvent() {
		return outputEvent;
	}

	public void setOutputEvent(String outputEvent) {
		this.outputEvent = outputEvent;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	
	public void setErrorType(String errorType) {
		this.errorType=errorType;
	}
	
	public String getErrorType() {
		return errorType;
	}
	
	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public void startEventLog() {
		this.startTime = this.getISODate(new Date());
	}

	
	public void stopEvent() throws ParseException {
		this.duration = getTimestamp(this.getEndTime())-getTimestamp(this.getStartTime());
	}
	public String getISODate(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat.format(date);
	}
	public Long getTimestamp(String stringDate) throws ParseException
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = dateFormat.parse(stringDate);
		
		return date.getTime();
	}
	public void loggerSetting(String id, String ioflow, String stepName, String string, String outputEvent,
			String eventType, Object payload) {
		this.correlationId = id;
		this.ioflow = ioflow;
		this.stepName=stepName;
		this.inputEvent=string;
		this.outputEvent=outputEvent;
		this.eventType=eventType;
		if (payload.equals(KafkaNull.INSTANCE)) {
			this.payload=null;
		}
		else {
			this.payload=payload;
		}
	}
}
