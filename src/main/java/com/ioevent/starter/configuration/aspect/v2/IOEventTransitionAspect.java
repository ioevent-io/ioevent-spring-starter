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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ioevent.starter.service.IOEventMessageBuilderService;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect class for advice associated with a @IOEvent calls for Transition task
 * event type
 */
@Slf4j
@Aspect
@Configuration

public class IOEventTransitionAspect {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private IOEventProperties iOEventProperties;

	@Autowired
	private IOEventService ioEventService;
	@Autowired
	private IOEventMessageBuilderService messageBuilderService;
	@Autowired
	private IOExceptionHandlingAspect ioExceptionHandlingAspect;

	/**
	 * Method AfterReturning advice runs after a successful completion of a
	 * Transition task with IOEvent annotation,
	 * 
	 * @param joinPoint    for the join point during the execution of the program,
	 * @param ioEvent      for ioevent annotation which include task information,
	 * @param returnObject for the returned object,
	 * @throws JsonProcessingException
	 * @throws ParseException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void transitionAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws JsonProcessingException, ParseException, InterruptedException, ExecutionException {
		if ((ioEvent.EventType() != EventTypesEnum.USER)&&(ioEvent.EventType() != EventTypesEnum.MANUAL)) {
			if (ioEventService.isTransition(ioEvent)) {
				IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
				EventLogger eventLogger = new EventLogger();
				eventLogger.startEventLog();
				StopWatch watch = ioeventRecordInfo.getWatch();
				IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
				ioeventRecordInfo.setWorkFlowName(
						ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
				String outputs = "";
				IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);
				IOResponse<Object> response = ioEventService.getpayload(joinPoint, returnObject);
				if (ioEvent.gatewayOutput().output().length != 0) {

					if (ioEvent.gatewayOutput().parallel()) {
						ioEventType = IOEventType.GATEWAY_PARALLEL;
						outputs = messageBuilderService.parallelEventSendProcess(eventLogger, ioEvent, ioFlow, response,
								outputs, ioeventRecordInfo, false);

					} else if (ioEvent.gatewayOutput().exclusive()) {
						ioEventType = IOEventType.GATEWAY_EXCLUSIVE;
						try {
							outputs = messageBuilderService.exclusiveEventSendProcess(eventLogger, ioEvent, ioFlow,
									returnObject, outputs, ioeventRecordInfo, false);
						} catch (IllegalStateException e) {
							ioExceptionHandlingAspect.throwingExceptionAspect(joinPoint, ioEvent, e);
							throw e;
						}
					}
				} else {
        if(ioEventService.isIntermediateTimer(ioEvent)){
					ioEventType = IOEventType.INTERMEDIATE_TIMER;
				}
					outputs = simpleEventSendProcess(eventLogger, ioEvent, ioFlow, response, outputs, ioeventRecordInfo,
							ioEventType);
				}

				prepareAndDisplayEventLogger(eventLogger, ioeventRecordInfo, ioEvent, outputs, watch,
						response.getBody(), ioEventType);
			}
		}
	}

	/**
	 * Method that build and send the event of a simple task,
	 * 
	 * @param eventLogger
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param response
	 * @param outputs           for the list of outputs of the event separated by
	 *                          ",",
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @param ioEventType       for the event type,
	 * @return string format list of outputs of the event separated by "," ,
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public String simpleEventSendProcess(EventLogger eventLogger, IOEvent ioEvent, IOFlow ioFlow,
			IOResponse<Object> response, String outputs, IOEventRecordInfo ioeventRecordInfo, IOEventType ioEventType)
			throws InterruptedException, ExecutionException {

		for (OutputEvent outputEvent : ioEventService.getOutputs(ioEvent)) {

			Message<Object> message;
			Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
					response.getHeaders());
			if (!StringUtils.isBlank(outputEvent.suffix())) {

				message = this.buildSuffixMessage(ioEvent, ioFlow, response, outputEvent, ioeventRecordInfo,
						ioeventRecordInfo.getStartTime(), ioEventType, headers);
				Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
				eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));

				outputs += ioeventRecordInfo.getOutputConsumedName() + outputEvent.suffix();
			} else {
				message = this.buildTransitionTaskMessage(ioEvent, ioFlow, response, outputEvent, ioeventRecordInfo,
						ioeventRecordInfo.getStartTime(), ioEventType, headers);
				Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
				eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));

				outputs += ioEventService.getOutputKey(outputEvent) + ",";
			}

		}
		return outputs;
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger       for the log info dto display,
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param output            for the output where the event will send ,
	 * @param watch             for capturing time,
	 * @param payload           for the payload of the event,
	 * @param ioEventType       for the event type,
	 * @throws JsonProcessingException
	 * @throws ParseException
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEventRecordInfo ioeventRecordInfo,
			IOEvent ioEvent, String output, StopWatch watch, Object payload, IOEventType ioEventType)
			throws JsonProcessingException, ParseException {
		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getOutputConsumedName(), output, ioEventType.toString(), payload);
		eventLogger.stopEvent();
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	/**
	 * Method that build the event message of simple Transition task to be send in
	 * kafka topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param response          for the response which include the payload of the
	 *                          event
	 * @param outputEvent       for the output Event where the event will send
	 * @param ioeventRecordInfo
	 * @param startTime         for the start time of the event,
	 * @param ioEventType       for the ioevent type
	 * @param headers           for message headers
	 * @return message type of Message,
	 */
	public Message<Object> buildTransitionTaskMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response,
			OutputEvent outputEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime, IOEventType ioEventType,
			Map<String, Object> headers) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, outputEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), ioEventService.getInputNames(ioEvent))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), ioEventService.getOutputKey(outputEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
	}

	/**
	 * Method that build the event message of add suffix task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param response          for the response which include the payload of the
	 *                          event
	 * @param outputEvent       for the output Event where the event will send
	 * @param ioeventRecordInfo
	 * @param startTime         for the start time of the event,
	 * @param ioEventType       for the ioevent type
	 * @param headers           for message headers
	 * @return message type of Message,
	 */
	public Message<Object> buildSuffixMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response,
			OutputEvent outputEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime, IOEventType ioEventType,
			Map<String, Object> headers) {
		String inputtopic = ioEventService.getInputEventByName(ioEvent, ioeventRecordInfo.getOutputConsumedName())
				.topic();
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, inputtopic);
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), ioeventRecordInfo.getOutputConsumedName())
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(),
						ioeventRecordInfo.getOutputConsumedName() + outputEvent.suffix())
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
	}
}
