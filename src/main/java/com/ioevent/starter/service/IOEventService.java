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




package com.ioevent.starter.service;






import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.annotations.IOPayload;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.annotations.OutputEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventParallelEventInformation;
import com.ioevent.starter.domain.IOEventType;

import lombok.extern.slf4j.Slf4j;

/**
 * class service for IOEvent,
 */
@Slf4j
@Service
public class IOEventService {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * This is a kafka producer which send parallel events info to
	 * ParallelEventTopic topic
	 * 
	 * @param parallelEventInfo for the parallel event information,
	 */
	public void sendParallelEventInfo(IOEventParallelEventInformation parallelEventInfo) {
		Message<IOEventParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ParallelEventTopic").setHeader(KafkaHeaders.MESSAGE_KEY,
						parallelEventInfo.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString()))
				.build();

		kafkaTemplate.send(message);
	}

	/**
	 * method returns all Inputs names of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of Inputs names,
	 */
	public List<String> getInputNames(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();

		for (InputEvent inputEvent : ioEvent.input()) {
			if (!StringUtils.isBlank(inputEvent.key() + inputEvent.value())) {
				if (!StringUtils.isBlank(inputEvent.value())) {
					result.add(inputEvent.value());
				} else {
					result.add(inputEvent.key());
				}
			}
		}

		for (InputEvent inputEvent : ioEvent.gatewayInput().input()) {
			if (!StringUtils.isBlank(inputEvent.key() + inputEvent.value())) {
				if (!StringUtils.isBlank(inputEvent.value())) {
					result.add(inputEvent.value());
				} else {
					result.add(inputEvent.key());
				}
			}
		}
		return result;
	}

	/**
	 * method returns all parallel Input names of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of Inputs names,
	 */
	public List<String> getParalleListInput(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();
		for (InputEvent inputEvent : ioEvent.gatewayInput().input()) {
			if (!StringUtils.isBlank(inputEvent.key() + inputEvent.value())) {
				if (!StringUtils.isBlank(inputEvent.value())) {
					result.add(inputEvent.value());
				} else {
					result.add(inputEvent.key());
				}
			}
		}
		return result;
	}

	/**
	 * method returns all outputs names of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of outputs names,
	 */
	public List<String> getOutputNames(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();

		for (OutputEvent outputEvent : ioEvent.output()) {
			if (!StringUtils.isBlank(outputEvent.key() + outputEvent.value())) {
				if (!StringUtils.isBlank(outputEvent.value())) {
					result.add(outputEvent.value());
				} else {
					result.add(outputEvent.key());
				}
			}
		}

		for (OutputEvent outputEvent : ioEvent.gatewayOutput().output()) {
			if (!StringUtils.isBlank(outputEvent.key() + outputEvent.value())) {
				if (!StringUtils.isBlank(outputEvent.value())) {
					result.add(outputEvent.value());
				} else {
					result.add(outputEvent.key());
				}
			}
		}
		return result;
	}

	/**
	 * method returns all output Event of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of OutputEvent Object ,
	 */
	public List<OutputEvent> getOutputs(IOEvent ioEvent) {
		List<OutputEvent> result = new ArrayList<>();

		for (OutputEvent outputEvent : ioEvent.output()) {
			if (!StringUtils.isBlank(outputEvent.key() + outputEvent.value() + outputEvent.suffix())) {
				result.add(outputEvent);
			}
		}

		for (OutputEvent outputEvent : ioEvent.gatewayOutput().output()) {
			if (!StringUtils.isBlank(outputEvent.key() + outputEvent.value() + outputEvent.suffix())) {
				result.add(outputEvent);
			}
		}
		return result;
	}

	/**
	 * method returns all inputs of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of InputEvent Object ,
	 */
	public List<InputEvent> getInputs(IOEvent ioEvent) {
		List<InputEvent> result = new ArrayList<>();

		for (InputEvent inputEvent : ioEvent.input()) {
			if (!StringUtils.isBlank(inputEvent.key() + inputEvent.value())) {
				result.add(inputEvent);
			}
		}

		for (InputEvent inputEvent : ioEvent.gatewayInput().input()) {
			if (!StringUtils.isBlank(inputEvent.key() + inputEvent.value())) {
				result.add(inputEvent);
			}
		}
		return result;
	}

	/**
	 * method returns all topics of @IOEvent annotation,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of Topics names ,
	 */

	public List<String> getTopics(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();
		if (!ioEvent.topic().equals("")) {
			result.add(ioEvent.topic());
		}
		for (InputEvent inputEvent : ioEvent.input()) {
			if (!inputEvent.topic().equals("")) {
				result.add(inputEvent.topic());
			}
		}
		for (OutputEvent outputEvent : ioEvent.output()) {
			if (!outputEvent.topic().equals("")) {
				result.add(outputEvent.topic());
			}
		}
		for (InputEvent inputEvent : ioEvent.gatewayInput().input()) {
			if (!inputEvent.topic().equals("")) {
				result.add(inputEvent.topic());
			}
		}
		for (OutputEvent outputEvent : ioEvent.gatewayOutput().output()) {
			if (!outputEvent.topic().equals("")) {
				result.add(outputEvent.topic());
			}
		}
		return result;
	}

	/**
	 * method returns all Input topics of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of Topics names ,
	 */

	public List<String> getInputTopic(IOEvent ioEvent, IOFlow ioFlow) {
		List<String> result = new ArrayList<>();
		if ((ioFlow != null) && !StringUtils.isBlank(ioFlow.topic())) {
			result.add(ioFlow.topic());
		}
		if (!StringUtils.isBlank(ioEvent.topic())) {
			result.add(ioEvent.topic());
		}
		for (InputEvent inputEvent : ioEvent.input()) {
			if (!StringUtils.isBlank(inputEvent.topic())) {
				result.add(inputEvent.topic());
			}
		}

		for (InputEvent inputEvent : ioEvent.gatewayInput().input()) {
			if (!StringUtils.isBlank(inputEvent.topic())) {
				result.add(inputEvent.topic());
			}
		}

		return result;
	}
	/**
	 * method returns all Input topics of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of Topics names ,
	 */

	public List<String> getOutputEventTopics(IOEvent ioEvent, IOFlow ioFlow) {
		List<String> result = new ArrayList<>();
		if ((ioFlow != null) && !StringUtils.isBlank(ioFlow.topic())) {
			result.add(ioFlow.topic());
		}
		if (!StringUtils.isBlank(ioEvent.topic())) {
			result.add(ioEvent.topic());
		}
		for (OutputEvent outputEvent : ioEvent.output()) {
			if (!StringUtils.isBlank(outputEvent.topic())) {
				result.add(outputEvent.topic());
			}
		}

		for (OutputEvent outputEvent : ioEvent.gatewayOutput().output()) {
			if (!StringUtils.isBlank(outputEvent.topic())) {
				result.add(outputEvent.topic());
			}
		}

		return result;
	}
	/**
	 * method returns if two lists are equal
	 * 
	 * @param firstList  list of String,
	 * @param secondList list of String,
	 * @return boolean ,
	 */
	public boolean sameList(List<String> firstList, List<String> secondList) {
		return (firstList.size() == secondList.size() && firstList.containsAll(secondList)
				&& secondList.containsAll(firstList));
	}

	/**
	 * method returns event type from the IOEvent annotation
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return IOEventType ,
	 */
	public IOEventType getIOEventType(IOEvent ioEvent) {
		if (!StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())) {
			return IOEventType.START;
		} else if (!StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())) {
			return IOEventType.END;
		} else {
			return IOEventType.TASK;
		}
	}

	/**
	 * method returns if the IOEvent annotation is of a Start Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isStart(IOEvent ioEvent) {
		return (!StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())
				&& (!getOutputs(ioEvent).isEmpty()));

	}

	/**
	 * method returns if the IOEvent annotation is of a End Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isEnd(IOEvent ioEvent) {
		return (!StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())
				&& (!getInputs(ioEvent).isEmpty()));
	}

	/**
	 * method returns if the IOEvent annotation is of a Implicit Task Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isImplicitTask(IOEvent ioEvent) {
		return ((getInputs(ioEvent).isEmpty() || getOutputs(ioEvent).isEmpty())
				&& (StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())
						&& StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())));

	}

	/**
	 * method returns if the IOEvent annotation is of a Task Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isTransition(IOEvent ioEvent) {
		return (StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())
				&& StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())
				&& !getInputs(ioEvent).isEmpty() && !getOutputs(ioEvent).isEmpty());
	}

	/**
	 * method returns input event of @IOEvent by name,
	 * 
	 * @param ioEvent    for the IOEvent annotation,
	 * @param InputName for the Input event name
	 * @return InputEvent ,
	 */
	public InputEvent getInputEventByName(IOEvent ioEvent, String inputName) {
		for (InputEvent inputEvent : getInputs(ioEvent)) {
			if (inputName.equals(inputEvent.key())) {
				return inputEvent;
			}
		}
		return null;
	}

	/**
	 * method returns Task specific type from the IOEvent annotation
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return IOEventType ,
	 */
	public IOEventType checkTaskType(IOEvent ioEvent) {
		IOEventType type = IOEventType.TASK;

		if ((ioEvent.gatewayOutput().output().length != 0) || (ioEvent.gatewayInput().input().length != 0)) {

			if (ioEvent.gatewayOutput().parallel() || ioEvent.gatewayInput().parallel()) {
				type = IOEventType.GATEWAY_PARALLEL;
			} else if (ioEvent.gatewayOutput().exclusive() || ioEvent.gatewayInput().exclusive()) {
				type = IOEventType.GATEWAY_EXCLUSIVE;
			}
		}

		return type;
	}

	/**
	 * method returns ID generated from @IOEvent elements ,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return String of ID generated ,
	 */
	public String generateID(IOEvent ioEvent) {

		return ioEvent.key().replaceAll("[^a-zA-Z ]", "").toLowerCase().replace(" ", "") + "-"
				+ getInputNames(ioEvent).hashCode() + "-" + getOutputNames(ioEvent).hashCode();
	}

	/**
	 * method returns ProcessName from @IOEvent ,@IOFlow and recordProcessName ,
	 * 
	 * @param ioEvent           for the IOEvent annotation,
	 * @param ioFlow            for the IOFlow annotation,
	 * @param recordProcessName for the process name consumed from record ,
	 * @return String of ProcessName ,
	 */
	public String getProcessName(IOEvent ioEvent, IOFlow ioFlow, String recordProcessName) {
		if (!StringUtils.isBlank(recordProcessName)) {
			return recordProcessName;

		} else if (!StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())) {
			if (!StringUtils.isBlank(ioEvent.startEvent().value())) {
				return ioEvent.startEvent().value();
			}
			return ioEvent.startEvent().key();
		} else if (!StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())) {
			if (!StringUtils.isBlank(ioEvent.endEvent().value())) {
				return ioEvent.endEvent().value();
			}
			return ioEvent.endEvent().key();

		} else if (!Objects.isNull(ioFlow)) {
			return ioFlow.name();
		}
		return "";
	}

	/**
	 * method returns output topic from @IOEvent ,@IOFlow and outputEventTopic ,
	 * 
	 * @param ioEvent          for the IOEvent annotation,
	 * @param ioFlow           for the IOFlow annotation,
	 * @param outputEventTopic for the output Event Topic name,
	 * @return String of TopicName ,
	 */
	public String getOutputTopicName(IOEvent ioEvent, IOFlow ioFlow, String outputEventTopic) {
		if (!StringUtils.isBlank(outputEventTopic)) {
			return outputEventTopic;
		} else if (!StringUtils.isBlank(ioEvent.topic())) {
			return ioEvent.topic();
		} else if ((ioFlow != null) && !StringUtils.isBlank(ioFlow.topic())) {
			return ioFlow.topic();

		} else {
			return "";
		}
	}

	/**
	 * method returns ApiKey from @IOFlow and IOEventProperties ,
	 * 
	 * @param ioFlow         for the IOFlow annotation,
	 * @param IOEventProperties for the IOEvent custom properties value ,
	 * @return String of ApiKey ,
	 */
	public String getApiKey(IOEventProperties iOEventProperties, IOFlow ioFlow) {
		if ((!Objects.isNull(ioFlow)) && (StringUtils.isNotBlank(ioFlow.apiKey()))) {
			return ioFlow.apiKey();
		} else if (StringUtils.isNotBlank(iOEventProperties.getApikey())) {
			return iOEventProperties.getApikey();
		}
		return "";
	}

	/**
	 * method returns Output Key from OutputEvent ,
	 * 
	 * @param OutputEvent for the OutputEvent annotation,
	 * @return String of Output Key ,
	 */
	public String getOutputKey(OutputEvent outputEvent) {
		if (!StringUtils.isBlank(outputEvent.value())) {
			return outputEvent.value();
		} else {
			return outputEvent.key();
		}
	}

	/**
	 * method returns payload of the method ,
	 * 
	 * @param joinPoint    for the JoinPoint where the method have been called ,
	 * @param returnObject for the object returned by the method
	 * @return IOResponse ,
	 */
	public IOResponse<Object> getpayload(JoinPoint joinPoint, Object returnObject) {
		try {
			if (returnObject != null) {
				IOResponse<Object> ioEventResponse = IOResponse.class.cast(returnObject);
				return ioEventResponse;
			}
			throw new NullPointerException();

		} catch (Exception e) {

			if (returnObject == null) {
				MethodSignature signature = (MethodSignature) joinPoint.getSignature();
				int ioPayloadIndex = getIOPayloadIndex(signature.getMethod());
				if (ioPayloadIndex >= 0) {
					return new IOResponse<>(null, joinPoint.getArgs()[ioPayloadIndex]);
				}
				return new IOResponse<>(null, joinPoint.getArgs()[0]);

			}
			return new IOResponse<>(null, returnObject);

		}
	}

	/**
	 * method returns IOPayload Annotation index in method parameters,
	 * 
	 * @param method for the method object ,
	 * @return int ,
	 */
	public int getIOPayloadIndex(Method method) {
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		int parameterIndex = 0;
		for (Annotation[] annotations : parameterAnnotations) {
			if (Arrays.asList(annotations).stream().filter(IOPayload.class::isInstance).count() != 0) {
				return parameterIndex;
			}

			parameterIndex++;
		}
		return -1;
	}

	/**
	 * method returns map of headers by merging the consumed headers with the new
	 * headers created in method,
	 * 
	 * @param headersConsumed for the List<Header> consumed from the event ,
	 * @param newHeaders      a Map<String, Object> for the new headers declared in
	 *                        method
	 * @return Map<String, Object> ,
	 */
	public Map<String, Object> prepareHeaders(List<Header> headersConsumed, Map<String, Object> newHeaders) {
		Map<String, Object> result = new HashMap<>();
		if (headersConsumed != null) {
			result = headersConsumed.stream().collect(Collectors.toMap(Header::key, h -> new String(h.value())));
		}
		result.putAll(newHeaders);
		return result;
	}
}
