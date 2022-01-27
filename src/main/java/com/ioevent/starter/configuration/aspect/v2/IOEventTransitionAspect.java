package com.ioevent.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
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
import com.ioevent.starter.annotations.TargetEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventService;
import com.ioevent.starter.service.IOEventContextHolder;

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

	/**
	 * Method AfterReturning advice runs after a successful completion of a
	 * Transition task with IOEvent annotation,
	 * 
	 * @param joinPoint    for the join point during the execution of the program,
	 * @param ioEvent      for ioevent annotation which include task information,
	 * @param returnObject for the returned object,
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void transitionAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws ParseException, JsonProcessingException {

		if (ioEventService.isTransition(ioEvent)) {
			IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			StopWatch watch = ioeventRecordInfo.getWatch();
			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			ioeventRecordInfo.setWorkFlowName(ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
			String targets = "";
			IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);
			IOResponse<Object> response =ioEventService.getpayload(joinPoint, returnObject);

			if (ioEvent.gatewayTarget().target().length != 0) {

				if (ioEvent.gatewayTarget().parallel()) {
					ioEventType = IOEventType.GATEWAY_PARALLEL;
					targets = parallelEventSendProcess(ioEvent, ioFlow, response, targets, ioeventRecordInfo);

				} else if (ioEvent.gatewayTarget().exclusive()) {
					ioEventType = IOEventType.GATEWAY_EXCLUSIVE;
					targets = exclusiveEventSendProcess(ioEvent, ioFlow, returnObject, targets, ioeventRecordInfo);

				}
			} else {

				targets = simpleEventSendProcess(ioEvent, ioFlow, response, targets, ioeventRecordInfo, ioEventType);
			}

			prepareAndDisplayEventLogger(eventLogger, ioeventRecordInfo, ioEvent, targets, watch, response.getBody(), ioEventType);
		}
	}

	

	/**
	 * Method that build and send the event of a simple task,
	 * 
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param ioflow         for ioflow annotation which include general
	 *                       information,
	 * @param returnObject   for the returned object,
	 * @param targets        for the list of targets of the event separated by ",",
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @param ioEventType    for the event type,
	 * @return string format list of targets of the event separated by "," ,
	 */
	public String simpleEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response, String targets,
			IOEventRecordInfo ioeventRecordInfo, IOEventType ioEventType) throws ParseException {

		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {

			Message<Object> message;
			Map<String, Object> headers=ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),response.getHeaders());
			if (!StringUtils.isBlank(targetEvent.suffix())) {

				message = this.buildSuffixMessage(ioEvent, ioFlow, response, targetEvent, ioeventRecordInfo,
						ioeventRecordInfo.getStartTime(), ioEventType,headers);
				kafkaTemplate.send(message);

				targets += ioeventRecordInfo.getTargetName() + targetEvent.suffix();
			} else {
				message = this.buildTransitionTaskMessage(ioEvent, ioFlow, response, targetEvent, ioeventRecordInfo,
						ioeventRecordInfo.getStartTime(), ioEventType,headers);
				kafkaTemplate.send(message);

				targets +=ioEventService.getTargetKey(targetEvent) + ",";
			}

		}
		return targets;
	}

	/**
	 * Method that build and send the event of a Exclusive Event task,
	 * 
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param ioflow         for ioflow annotation which include general
	 *                       information,
	 * @param returnObject   for the returned object,
	 * @param targets        for the list of targets of the event separated by ",",
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @return string format list of targets of the event separated by "," ,
	 */
	public String exclusiveEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, Object returnObject, String targets,
			IOEventRecordInfo ioeventRecordInfo) throws ParseException {

		IOResponse<Object> ioEventResponse = IOResponse.class.cast(returnObject);
		Map<String, Object> headers=ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),ioEventResponse.getHeaders());
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			if (ioEventResponse.getString().equals(ioEventService.getTargetKey(targetEvent))) {
				Message<Object> message = this.buildTransitionGatewayExclusiveMessage(ioEvent, ioFlow,
						ioEventResponse, targetEvent, ioeventRecordInfo, ioeventRecordInfo.getStartTime(),headers);
				kafkaTemplate.send(message);

				targets +=ioEventService.getTargetKey(targetEvent) + ",";
				log.info("sent to : {}",ioEventService.getTargetKey(targetEvent));
			}

		}
		return targets;
	}

	/**
	 * Method that build and send the event of a Parallel Event task,
	 * 
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param ioflow         for ioflow annotation which include general
	 *                       information,
	 * @param returnObject   for the returned object,
	 * @param targets        for the list of targets of the event separated by ",",
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @return string format list of targets of the event separated by "," ,
	 */
	public String parallelEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response, String targets,
			IOEventRecordInfo ioeventRecordInfo) {
		Map<String, Object> headers=ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),response.getHeaders());
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			Message<Object> message = this.buildTransitionGatewayParallelMessage(ioEvent, ioFlow, response,
					targetEvent, ioeventRecordInfo, ioeventRecordInfo.getStartTime(),headers);
			kafkaTemplate.send(message);

			targets += ioEventService.getTargetKey(targetEvent)+ ",";
		}
		return targets;
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger    for the log info dto display,
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param target         for the target where the event will send ,
	 * @param watch          for capturing time,
	 * @param payload        for the payload of the event,
	 * @param ioEventType    for the event type,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEventRecordInfo ioeventRecordInfo, IOEvent ioEvent,
			String target, StopWatch watch, Object payload, IOEventType ioEventType) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getTargetName(), target, ioEventType.toString(), payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	/**
	 * Method that build the event message of simple Transition task to be send in
	 * kafka topic,
	 * 
	 * @param ioEvent         for ioevent annotation which include task information,
	 * @param ioflow          for ioflow annotation which include general
	 *                        information,
	 * @param payload         for the payload of the event,
	 * @param processName     for the process name
	 * @param uuid            for the correlation_id,
	 * @param targetEventName for the target Event where the event will send ,
	 * @param targetTopic     for the name of the target topic ,
	 * @param startTime       for the start time of the event,
	 * @return message type of Message,
	 */
	public Message<Object> buildTransitionTaskMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response,
			TargetEvent targetEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime, IOEventType ioEventType,Map<String, Object> headers) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers).setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), ioEventService.getSourceNames(ioEvent))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(),ioEventService.getTargetKey(targetEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime).build();
	}

	/**
	 * Method that build the event message of Parallel task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent         for ioevent annotation which include task information,
	 * @param ioflow          for ioflow annotation which include general
	 *                        information,
	 * @param payload         for the payload of the event,
	 * @param processName     for the process name
	 * @param uuid            for the correlation_id,
	 * @param targetEventName for the target Event where the event will send ,
	 * @param targetTopic     for the name of the target topic ,
	 * @param startTime       for the start time of the event,
	 * @return message type of Message,
	 */
	public Message<Object> buildTransitionGatewayParallelMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response,
			TargetEvent targetEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime,Map<String, Object> headers) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers).setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), ioEventService.getSourceNames(ioEvent))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(),ioEventService.getTargetKey(targetEvent) )
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime).build();
	}

	/**
	 * Method that build the event message of Exclusive task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent         for ioevent annotation which include task information,
	 * @param ioflow          for ioflow annotation which include general
	 *                        information,
	 * @param payload         for the payload of the event,
	 * @param processName     for the process name
	 * @param uuid            for the correlation_id,
	 * @param targetEventName for the target Event where the event will send ,
	 * @param targetTopic     for the name of the target topic ,
	 * @param startTime       for the start time of the event,
	 * @return message type of Message,
	 */
	public Message<Object> buildTransitionGatewayExclusiveMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response,
			TargetEvent targetEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime,Map<String, Object> headers) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers).setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.GATEWAY_EXCLUSIVE.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), ioEventService.getSourceNames(ioEvent))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(),ioEventService.getTargetKey(targetEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime).build();
	}

	/**
	 * Method that build the event message of add suffix task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent         for ioevent annotation which include task information,
	 * @param ioflow          for ioflow annotation which include general
	 *                        information,
	 * @param payload         for the payload of the event,
	 * @param processName     for the process name
	 * @param uuid            for the correlation_id,
	 * @param targetEventName for the target Event where the event will send ,
	 * @param targetTopic     for the name of the target topic ,
	 * @param startTime       for the start time of the event,
	 * @return message type of Message,
	 */
	public Message<Object> buildSuffixMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response, TargetEvent targetEvent,
			IOEventRecordInfo ioeventRecordInfo, Long startTime, IOEventType ioEventType,Map<String, Object> headers) {
		String sourcetopic = ioEventService.getSourceEventByName(ioEvent, ioeventRecordInfo.getTargetName()).topic();
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, sourcetopic);
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers).setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), ioeventRecordInfo.getTargetName())
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(),
						ioeventRecordInfo.getTargetName() + targetEvent.suffix())
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime).build();
	}
}
