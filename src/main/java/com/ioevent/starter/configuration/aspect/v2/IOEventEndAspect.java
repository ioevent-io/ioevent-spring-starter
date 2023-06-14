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
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
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
 * Aspect class for advice associated with a @IOEvent calls for End task event
 * type
 */
@Slf4j
@Aspect
@Configuration
@ConditionalOnExpression("${false}")
public class IOEventEndAspect {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private IOEventProperties iOEventProperties;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private IOEventService ioEventService;

	/**
	 * Method AfterReturning advice runs after a successful completion of a End task
	 * with IOEvent annotation,
	 * 
	 * @param joinPoint    for the join point during the execution of the program,
	 * @param ioEvent      for ioevent annotation which include task information,
	 * @param returnObject for the returned object,
	 * @throws JsonProcessingException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws ParseException
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws JsonProcessingException, InterruptedException, ExecutionException, ParseException {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		IOEvent myAnnotation = signature.getMethod().getAnnotation(IOEvent.class);
		if ((myAnnotation.EventType() != EventTypesEnum.USER)&&(myAnnotation.EventType() != EventTypesEnum.MANUAL)) {
			if (ioEventService.isEnd(ioEvent)) {
				IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
				Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
						ioEventService.getpayload(joinPoint, returnObject).getHeaders());

				StopWatch watch = ioeventRecordInfo.getWatch();
				EventLogger eventLogger = new EventLogger();
				eventLogger.startEventLog();
				eventLogger.setStartTime(eventLogger.getISODate(new Date(ioeventRecordInfo.getStartTime())));
				IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
				ioeventRecordInfo.setWorkFlowName(
						ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
				IOResponse<Object> payload = ioEventService.getpayload(joinPoint, returnObject);
				String output = "END";
				Message<Object> message = this.buildEventMessage(ioEvent, ioFlow, payload, output, ioeventRecordInfo,
						ioeventRecordInfo.getStartTime(), headers,"");
				Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
				eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));
				prepareAndDisplayEventLogger(eventLogger, ioEvent, payload.getBody(), watch, ioeventRecordInfo);
			}
		}
	}

	/**
	 * Method that build the event message of End task to be send in kafka topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param payload           for the payload of the event,
	 * @param outputEvent       for the output Event where the event will send ,
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @param startTime         for the start time of the event,
	 * @param headers
	 * @return message type of Message,
	 */
	public Message<Object> buildEventMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload,
			String outputEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime, Map<String, Object> headers,String key) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, "");
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		return MessageBuilder.withPayload(payload.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.MESSAGE_KEY.toString(), key)
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), outputEvent)
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.END.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), Arrays.asList(ioeventRecordInfo.getOutputConsumedName()))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
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
	 * @throws ParseException
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent, Object payload, StopWatch watch,
			IOEventRecordInfo ioeventRecordInfo) throws JsonProcessingException, ParseException {

		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getOutputConsumedName(), "__", "End", payload);
		eventLogger.stopEvent();
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
