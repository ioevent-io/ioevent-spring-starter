package com.grizzlywave.starter.configuration.aspect.v2;

import java.text.ParseException;

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
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.IOEventResponse;
import com.grizzlywave.starter.annotations.v2.IOFlow;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventHeaders;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

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
	private WaveProperties waveProperties;

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
			WaveRecordInfo waveRecordInfo = WaveContextHolder.getContext();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			StopWatch watch = waveRecordInfo.getWatch();
			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			waveRecordInfo
					.setWorkFlowName(ioEventService.getProcessName(ioEvent, ioFlow, waveRecordInfo.getWorkFlowName()));
			String targets = "";
			IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);
			Object payload = getpayload(joinPoint, returnObject);

			if (ioEvent.gatewayTarget().target().length != 0) {

				if (ioEvent.gatewayTarget().parallel()) {
					ioEventType = IOEventType.GATEWAY_PARALLEL;
					targets = parallelEventSendProcess(ioEvent, ioFlow, payload, targets, waveRecordInfo);

				} else if (ioEvent.gatewayTarget().exclusive()) {
					ioEventType = IOEventType.GATEWAY_EXCLUSIVE;
					targets = exclusiveEventSendProcess(ioEvent, ioFlow, payload, targets, waveRecordInfo);

				}
			} else {

				targets = simpleEventSendProcess(ioEvent, ioFlow, payload, targets, waveRecordInfo, ioEventType);
			}

			prepareAndDisplayEventLogger(eventLogger, waveRecordInfo, ioEvent, targets, watch, payload, ioEventType);
		}
	}

	/**
	 * Method that returns event payload according to method parameters and return
	 * object,
	 * 
	 * @param joinPoint    for the point during the execution of the program,
	 * @param returnObject for the returned object,
	 * @return An object of type Object,
	 */
	public Object getpayload(JoinPoint joinPoint, Object returnObject) {
		if (returnObject == null) {
			return joinPoint.getArgs()[0];

		}
		return returnObject;
	}

	/**
	 * Method that build and send the event of a simple task,
	 * 
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param ioflow         for ioflow annotation which include general
	 *                       information,
	 * @param returnObject   for the returned object,
	 * @param targets        for the list of targets of the event separated by ",",
	 * @param waveRecordInfo for the record information from the consumed event,
	 * @param ioEventType    for the event type,
	 * @return string format list of targets of the event separated by "," ,
	 */
	public String simpleEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo, IOEventType ioEventType) throws ParseException {

		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {

			Message<Object> message;

			if (!StringUtils.isBlank(targetEvent.suffix())) {

				message = this.buildSuffixMessage(ioEvent, ioFlow, returnObject, targetEvent, waveRecordInfo,
						waveRecordInfo.getStartTime(), ioEventType);
				kafkaTemplate.send(message);

				targets += waveRecordInfo.getTargetName() + targetEvent.suffix();
			} else {
				message = this.buildTransitionTaskMessage(ioEvent, ioFlow, returnObject, targetEvent, waveRecordInfo,
						waveRecordInfo.getStartTime(), ioEventType);
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
	 * @param waveRecordInfo for the record information from the consumed event,
	 * @return string format list of targets of the event separated by "," ,
	 */
	public String exclusiveEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo) throws ParseException {

		IOEventResponse<Object> ioEventResponse = IOEventResponse.class.cast(returnObject);
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			if (ioEventResponse.getString().equals(ioEventService.getTargetKey(targetEvent))) {
				Message<Object> message = this.buildTransitionGatewayExclusiveMessage(ioEvent, ioFlow,
						ioEventResponse.getBody(), targetEvent, waveRecordInfo, waveRecordInfo.getStartTime());
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
	 * @param waveRecordInfo for the record information from the consumed event,
	 * @return string format list of targets of the event separated by "," ,
	 */
	public String parallelEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo) {
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			Message<Object> message = this.buildTransitionGatewayParallelMessage(ioEvent, ioFlow, returnObject,
					targetEvent, waveRecordInfo, waveRecordInfo.getStartTime());
			kafkaTemplate.send(message);

			targets += ioEventService.getTargetKey(targetEvent)+ ",";
		}
		return targets;
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger    for the log info dto display,
	 * @param waveRecordInfo for the record information from the consumed event,
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param target         for the target where the event will send ,
	 * @param watch          for capturing time,
	 * @param payload        for the payload of the event,
	 * @param ioEventType    for the event type,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, WaveRecordInfo waveRecordInfo, IOEvent ioEvent,
			String target, StopWatch watch, Object payload, IOEventType ioEventType) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(waveRecordInfo.getId(), waveRecordInfo.getWorkFlowName(), ioEvent.key(),
				waveRecordInfo.getTargetName(), target, ioEventType.toString(), payload);
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
	public Message<Object> buildTransitionTaskMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload,
			TargetEvent targetEvent, WaveRecordInfo waveRecordInfo, Long startTime, IOEventType ioEventType) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetEvent.topic());
		String apiKey = ioEventService.getApiKey(waveProperties, ioFlow);

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), waveRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), waveRecordInfo.getId())
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
	public Message<Object> buildTransitionGatewayParallelMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload,
			TargetEvent targetEvent, WaveRecordInfo waveRecordInfo, Long startTime) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetEvent.topic());
		String apiKey = ioEventService.getApiKey(waveProperties, ioFlow);

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), waveRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), waveRecordInfo.getId())
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
	public Message<Object> buildTransitionGatewayExclusiveMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload,
			TargetEvent targetEvent, WaveRecordInfo waveRecordInfo, Long startTime) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetEvent.topic());
		String apiKey = ioEventService.getApiKey(waveProperties, ioFlow);

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), waveRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), waveRecordInfo.getId())
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
	public Message<Object> buildSuffixMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload, TargetEvent targetEvent,
			WaveRecordInfo waveRecordInfo, Long startTime, IOEventType ioEventType) {
		String sourcetopic = ioEventService.getSourceEventByName(ioEvent, waveRecordInfo.getTargetName()).topic();
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, sourcetopic);
		String apiKey = ioEventService.getApiKey(waveProperties, ioFlow);

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), waveRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), waveRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), waveRecordInfo.getTargetName())
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(),
						waveRecordInfo.getTargetName() + targetEvent.suffix())
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime).build();
	}
}
