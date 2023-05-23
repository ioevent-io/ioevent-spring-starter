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

package com.ioevent.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.OutputEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.enums.EventTypesEnum;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect class for advice associated with a @IOEvent calls for Start task event
 * type
 */
@Slf4j
@Aspect
@Configuration
@ConditionalOnExpression("${false}")
public class IOEventStartAspect {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private IOEventProperties iOEventProperties;
	@Autowired
	private IOEventService ioEventService;

	/**
	 * Method Before advice runs after a successful completion of a Start task with
	 * IOEvent annotation,
	 * 
	 * @param joinPoint for the join point during the execution of the program,
	 * @param ioEvent   for ioevent annotation which include task information,
	 * @throws JsonProcessingException
	 * @throws ParseExceptions
	 */
	@Before(value = "@annotation(anno)", argNames = "jp, anno")
	public void iOEventAnnotationImpicitStartAspect(JoinPoint joinPoint, IOEvent ioEvent)
			throws ParseException, JsonProcessingException {		
		if ((ioEvent.EventType() != EventTypesEnum.USER)&&(ioEvent.EventType() != EventTypesEnum.MANUAL)) {
			if (ioEventService.isStart(ioEvent)) {
				StopWatch watch = new StopWatch();
				watch.start("IOEvent annotation Start Aspect");
				IOEventContextHolder.setContext(new IOEventRecordInfo("", "", "", watch, (new Date()).getTime(), ""));
			}
		}
	}

	/**
	 * Method AfterReturning advice runs after a successful completion of a Start
	 * task with IOEvent annotation,
	 * 
	 * @param joinPoint    for the join point during the execution of the program,
	 * @param ioEvent      for ioevent annotation which include task information,
	 * @param returnObject for the returned object,
	 * @throws ParseException
	 * @throws JsonProcessingException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws ParseException, JsonProcessingException, InterruptedException, ExecutionException {

		if ((ioEvent.EventType() != EventTypesEnum.USER)&&(ioEvent.EventType() != EventTypesEnum.MANUAL)) {
			if (ioEventService.isStart(ioEvent)) {
				EventLogger eventLogger = new EventLogger();
				IOEventRecordInfo ioeventRecordInfoInput = IOEventContextHolder.getContext();
				StopWatch watch = ioeventRecordInfoInput.getWatch();
				eventLogger.startEventLog();
				eventLogger.setStartTime(eventLogger.getISODate(new Date(ioeventRecordInfoInput.getStartTime())));
				IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
				UUID uuid = UUID.randomUUID();
			  StringBuilder output = new StringBuilder();
				IOResponse<Object> response = ioEventService.getpayload(joinPoint, returnObject);
				String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");

				for (OutputEvent outputEvent : ioEventService.getOutputs(ioEvent)) {
					Message<Object> message = this.buildStartMessage(ioEvent, ioFlow, response, processName,
							uuid.toString(), outputEvent, eventLogger.getTimestamp(eventLogger.getStartTime()));
					Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
					eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));

				output.append(ioEventService.getOutputKey(outputEvent)).append(",");
				}
				prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent, processName, output.toString(), response.getBody(),
						watch);
			}

		}
	}

	/**
	 * Method that build the event message of Start task to be send in kafka topic,
	 * 
	 * @param ioEvent     for ioevent annotation which include task information,
	 * @param ioFlow      for ioflow annotation which include general information,
	 * @param response    for the IOResponse
	 * @param processName for the process name
	 * @param uuid        for the correlation_id,
	 * @param outputEvent for the output Event where the event will send ,
	 * @param startTime   for the start time of the event,
	 * @return message type of Message,
	 */
	public Message<Object> buildStartMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response,
			String processName, String uuid, OutputEvent outputEvent, Long startTime) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, outputEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		return MessageBuilder.withPayload(response.getBody()).copyHeaders(response.getHeaders())
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, uuid).setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventService.getIOEventType(ioEvent).toString())
				.setHeader(IOEventHeaders.INPUT.toString(), new ArrayList<String>(Arrays.asList("Start")))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), ioEventService.getOutputKey(outputEvent))
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), processName)
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger for the log info dto display,
	 * @param uuid        for the correlation_id,
	 * @param ioEvent     for ioevent annotation which include task information,
	 * @param processName for the process name
	 * @param output      for the output where the event will send ,
	 * @param payload     for the payload of the event,
	 * @param watch       for capturing time,
	 * @throws ParseException
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, UUID uuid, IOEvent ioEvent, String processName,
			String output, Object payload, StopWatch watch) throws JsonProcessingException, ParseException {
		watch.stop();
		eventLogger.loggerSetting(uuid.toString(), processName, ioEvent.key(), null, output, "Init", payload);
		eventLogger.stopEvent();
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

}
