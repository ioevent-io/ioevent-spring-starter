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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaNull;
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
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventMessageBuilderService;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect class for advice associated with a @IOEvent calls for Implicit task
 * event type
 */
@Slf4j
@Aspect
@Configuration
@ConditionalOnExpression("${false}")
public class IOEvenImplicitTaskAspect {
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private IOEventMessageBuilderService messageBuilderService;
	@Autowired
	private IOEventProperties iOEventProperties;
	@Autowired
	private IOEventService ioEventService;
	@Value("${spring.application.name}")
	private String appName;

	private static final String END_PREFIX = "end_Event";
	private static final String START_PREFIX = "start-to-";

	/**
	 * Before advice runs before the execution of an Implicit task with IOEvent
	 * annotation,
	 * 
	 * @param joinPoint for the join point during the execution of the program,
	 * @param ioEvent   for ioevent annotation which include task information,
	 * @throws JsonProcessingException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	@Before(value = "@annotation(anno)", argNames = "jp, anno")
	public void iOEventAnnotationImpicitStartAspect(JoinPoint joinPoint, IOEvent ioEvent)
			throws ParseException, JsonProcessingException, InterruptedException, ExecutionException {

		if (ioEventService.isImplicitTask(ioEvent) && ioEventService.getInputs(ioEvent).isEmpty()) {

			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			IOResponse<Object> response = new IOResponse<>(null, KafkaNull.INSTANCE, null);
			StopWatch watch = new StopWatch();
			UUID uuid = UUID.randomUUID();
			System.out.println(uuid);
			watch.start("IOEvent annotation Implicit Start");
			// IOEventContextHolder.setContext(new IOEventRecordInfo(uuid.toString(), "",
			// "", watch,
			// eventLogger.getTimestamp(eventLogger.getStartTime())));
			String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");
			String outputKey = START_PREFIX + ioEvent.key();
			// List<String> topics = ioEventService.getOutputEventTopics(ioEvent, ioFlow);
			Message<Object> message = this.buildImplicitStartMessage(ioFlow, response, processName, uuid.toString(),
					outputKey, eventLogger.getTimestamp(eventLogger.getStartTime()), "");
			Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
			eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));
			prepareAndDisplayEventLogger(eventLogger, uuid.toString(), ioEvent, processName, outputKey, response,
					watch);
			IOEventContextHolder.setContext(new IOEventRecordInfo(uuid.toString(), "", "", watch,
					eventLogger.getTimestamp(eventLogger.getStartTime()), eventLogger.getEndTime()));
		}
	}

	/**
	 * Method AfterReturning advice runs after a successful completion of a Implicit
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
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		IOEvent myAnnotation = signature.getMethod().getAnnotation(IOEvent.class);
			if (ioEventService.isImplicitTask(ioEvent)) {

				IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
				EventLogger eventLogger = new EventLogger();
				IOEventRecordInfo ioeventRecordInfoInput = IOEventContextHolder.getContext();
				eventLogger.startEventLog();
				IOResponse<Object> response = ioEventService.getpayload(joinPoint, returnObject);
				StopWatch watch = new StopWatch();
				String output = "";
				IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);
				String messageKey = "";
				if (ioEventService.isMessage(ioEvent)) {
					messageKey = ioEvent.message().key();
				}
				if (!ioEventService.getInputs(ioEvent).isEmpty()) {
					IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
					watch = ioeventRecordInfo.getWatch();
					eventLogger.setStartTime(eventLogger.getISODate(new Date(ioeventRecordInfo.getStartTime())));
					Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
							response.getHeaders());
					ioeventRecordInfo.setWorkFlowName(
							ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));

					Message<Object> message = this.buildMessage(ioEvent, ioFlow, response,
							ioeventRecordInfo.getWorkFlowName(), ioeventRecordInfo.getId(), END_PREFIX, "",
							eventLogger.getTimestamp(eventLogger.getStartTime()),
							ioeventRecordInfo.getInstanceStartTime(), ioEventType, headers, messageKey);

					Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
					eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));

					prepareAndDisplayEventLogger(eventLogger, ioEvent, ioeventRecordInfo, response, END_PREFIX,
							ioEventType, watch);
					ioeventRecordInfoInput.setOutputConsumedName(END_PREFIX);
					createImpliciteEndEvent(ioEvent, ioFlow, ioeventRecordInfoInput, response, eventLogger);

				} else if (!ioEventService.getOutputs(ioEvent).isEmpty()) {
					watch = ioeventRecordInfoInput.getWatch();
					eventLogger.setStartTime(ioeventRecordInfoInput.getLastEventEndTime());
					// watch.start("IOEvent annotation Implicit TASK Aspect");
					ioeventRecordInfoInput.setWorkFlowName(ioEventService.getProcessName(ioEvent, ioFlow, ""));
					Map<String, Object> headers = ioEventService.prepareHeaders(null, response.getHeaders());
					if (ioEvent.gatewayOutput().output().length != 0) {

						if (ioEvent.gatewayOutput().parallel()) {
							ioEventType = IOEventType.GATEWAY_PARALLEL;
							output = messageBuilderService.parallelEventSendProcess(eventLogger, ioEvent, ioFlow,
									response, output, ioeventRecordInfoInput, true);

						} else if (ioEvent.gatewayOutput().exclusive()) {
							ioEventType = IOEventType.GATEWAY_EXCLUSIVE;
							output = messageBuilderService.exclusiveEventSendProcess(eventLogger, ioEvent, ioFlow,
									returnObject, output, ioeventRecordInfoInput, true);

						}
					} else {

						for (OutputEvent outputEvent : ioEventService.getOutputs(ioEvent)) {
							String outputKey = ioEventService.getOutputKey(outputEvent);
							String outputTopic = outputEvent.topic();
							if (outputEvent.userActionRequired()){
								outputTopic = appName+ "_" + "ioevent-user-task";
							}
							Message<Object> message = this.buildMessage(ioEvent, ioFlow, response,
									ioeventRecordInfoInput.getWorkFlowName(), ioeventRecordInfoInput.getId(), outputKey,
									outputTopic, eventLogger.getTimestamp(eventLogger.getStartTime()),
									ioeventRecordInfoInput.getInstanceStartTime(), ioEventType, headers, messageKey);
							Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
							eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));

							output += outputKey + ",";
						}
					}

					ioeventRecordInfoInput.setOutputConsumedName(START_PREFIX + ioEvent.key());
					prepareAndDisplayEventLogger(eventLogger, ioEvent, ioeventRecordInfoInput, response, output,
							ioEventType, watch);
				} else {
					watch = ioeventRecordInfoInput.getWatch();
					eventLogger.setStartTime(ioeventRecordInfoInput.getLastEventEndTime());

					// watch.start("IOEvent annotation Implicit TASK Aspect");
					Map<String, Object> headers = ioEventService.prepareHeaders(null, response.getHeaders());
					Message<Object> message = this.buildMessage(ioEvent, ioFlow, response, ioFlow.name(),
							ioeventRecordInfoInput.getId(), END_PREFIX, "",
							eventLogger.getTimestamp(eventLogger.getStartTime()),
							ioeventRecordInfoInput.getInstanceStartTime(), ioEventType, headers, messageKey);
					Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
					eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));
					ioeventRecordInfoInput.setWorkFlowName(ioFlow.name());
					ioeventRecordInfoInput.setOutputConsumedName(START_PREFIX + ioEvent.key());
					prepareAndDisplayEventLogger(eventLogger, ioEvent, ioeventRecordInfoInput, response,
							END_PREFIX /* + ioEvent.key() */, ioEventType, watch);
					ioeventRecordInfoInput.setOutputConsumedName(END_PREFIX);
					createImpliciteEndEvent(ioEvent, ioFlow, ioeventRecordInfoInput, response, eventLogger);
				}

			}
	}

	public void createImpliciteEndEvent(IOEvent ioEvent, IOFlow ioFlow, IOEventRecordInfo ioeventRecordInfo,
			IOResponse<Object> response, EventLogger eventLogger)
			throws ParseException, JsonProcessingException, InterruptedException, ExecutionException {
		StopWatch watch = new StopWatch();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Implicit End");
		Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
				response.getHeaders());
		Message<Object> message = this.buildEndMessage(ioFlow, response, ioeventRecordInfo.getWorkFlowName(),
				ioeventRecordInfo.getId(), eventLogger.getTimestamp(eventLogger.getStartTime()),
				ioeventRecordInfo.getInstanceStartTime(), headers, "");

		Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
		eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));
		prepareAndDisplayEventLogger(eventLogger, ioEvent, response, watch, ioeventRecordInfo);
	}

	/**
	 * Method that build the event message of Implicit task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param payload           for the payload of the event,
	 * @param processName       for the process name
	 * @param uuid              for the correlation_id,
	 * @param outputEventName   for the output Event where the event will send ,
	 * @param outputTopic       for the name of the output topic ,
	 * @param startTime         for the start time of the event,
	 * @param instanceStartTime
	 * @param ioEventType
	 * @param headers
	 * @return message type of Message,
	 */
	public Message<Object> buildMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload, String processName,
			String uuid, String outputEventName, String outputTopic, Long startTime, Long instanceStartTime,
			IOEventType ioEventType, Map<String, Object> headers, String key) {
		boolean isStartImplicit = ioEventService.getInputs(ioEvent).isEmpty();
		boolean isEndImplicit = ioEventService.getOutputs(ioEvent).isEmpty();
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, outputTopic);
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		List<String> inputEvents = ioEventService.getInputNames(ioEvent);
		if (ioEventType.equals(IOEventType.END)) {
			inputEvents = Arrays.asList(END_PREFIX);

		} else if (inputEvents.isEmpty()) {
			inputEvents.add(START_PREFIX + ioEvent.key());
		}
		return MessageBuilder.withPayload(payload.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.KEY, uuid).setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.STEP_NAME.toString(),
						ioEventType.equals(IOEventType.END) ? "END-EVENT" : ioEvent.key())
				.setHeader(IOEventHeaders.MESSAGE_KEY.toString(), key)
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), inputEvents)
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), outputEventName)
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), processName)
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), instanceStartTime)
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), isStartImplicit)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), isEndImplicit).build();
	}

	public Message<Object> buildImplicitStartMessage(IOFlow ioFlow, IOResponse<Object> payload, String processName,
			String uuid, String outputEventName, Long startTime, String key) {
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		return MessageBuilder.withPayload(payload.getBody()).setHeader(KafkaHeaders.TOPIC, "ioevent-implicit-topic")
				.setHeader(KafkaHeaders.KEY, uuid)
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.MESSAGE_KEY.toString(), key)
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "START-EVENT")
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.START.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), new ArrayList<String>(Arrays.asList("Start")))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), outputEventName)
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), processName)
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), true)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();

	}

	public Message<Object> buildEndMessage(IOFlow ioFlow, IOResponse<Object> payload, String processName, String uuid,
			Long startTime, Long instanceStartTime, Map<String, Object> headers, String key) {

		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(payload.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-implicit-topic")
				.setHeader(KafkaHeaders.KEY, uuid)
				.setHeader(IOEventHeaders.MESSAGE_KEY.toString(), key)
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "END-EVENT")
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.END.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), Arrays.asList(END_PREFIX))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), "END")
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), processName)
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), instanceStartTime)
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger       for the log info dto display,
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioEventRecordInfo
	 * @param payload           for the payload of the event,
	 * @param outputName
	 * @param ioEventType       for the IOEvent Type,
	 * @param watch             for capturing time,
	 * @throws JsonProcessingException
	 * @throws ParseException
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent,
			IOEventRecordInfo ioEventRecordInfo, IOResponse<Object> payload, String outputName, IOEventType ioEventType,
			StopWatch watch) throws JsonProcessingException, ParseException {
		watch.stop();
		eventLogger.loggerSetting(ioEventRecordInfo.getId(), ioEventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioEventRecordInfo.getOutputConsumedName(), outputName, ioEventType.toString(), payload.getBody());
		eventLogger.stopEvent();
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger       for the log info dto display,
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param payload           for the payload of the event,
	 * @param watch             for capturing time,
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @throws JsonProcessingException
	 * @throws ParseException
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent, IOResponse<Object> payload,
			StopWatch watch, IOEventRecordInfo ioeventRecordInfo) throws JsonProcessingException, ParseException {

		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getOutputConsumedName(), "__", "End", payload.getBody());
		eventLogger.stopEvent();
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger for the log info dto display,
	 * @param uuid        for the correlation_id,
	 * @param ioEvent     for ioevent annotation which include task information,
	 * @param processName for the process name,
	 * @param output      for the output where the event will send ,
	 * @param payload     for the payload of the event,
	 * @param watch       for capturing time,
	 * @throws JsonProcessingException
	 * @throws ParseException
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, String uuid, IOEvent ioEvent, String processName,
			String output, IOResponse<Object> payload, StopWatch watch) throws JsonProcessingException, ParseException {
		watch.stop();
		eventLogger.loggerSetting(uuid, processName, ioEvent.key(), null, output, "START", payload.getBody());
		eventLogger.stopEvent();

		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
		watch.start("IOEvent annotation Implicit TASK Aspect");

	}
}
