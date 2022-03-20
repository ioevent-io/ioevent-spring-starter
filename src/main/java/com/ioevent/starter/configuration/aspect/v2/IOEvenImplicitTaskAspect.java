package com.ioevent.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect class for advice associated with a @IOEvent calls for Implicit task
 * event type
 */
@Slf4j
@Aspect
@Configuration
public class IOEvenImplicitTaskAspect {
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private IOEventProperties iOEventProperties;
	@Autowired
	private IOEventService ioEventService;

	private static final String END_PREFIX = "end-from-";
	private static final String START_PREFIX = "start-to-";

	/**
	 * Before advice runs before the execution of an Implicit task with IOEvent
	 * annotation,
	 * 
	 * @param joinPoint for the join point during the execution of the program,
	 * @param ioEvent   for ioevent annotation which include task information,
	 */
	@Before(value = "@annotation(anno)", argNames = "jp, anno")
	public void iOEventAnnotationImpicitStartAspect(JoinPoint joinPoint, IOEvent ioEvent)
			throws ParseException, JsonProcessingException {

		if (ioEventService.isImplicitTask(ioEvent) && ioEventService.getSources(ioEvent).isEmpty()) {

			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			IOResponse<Object> response = new IOResponse<>(null, "", null);
			StopWatch watch = new StopWatch();
			UUID uuid = UUID.randomUUID();
			IOEventContextHolder.setContext(new IOEventRecordInfo(uuid.toString(), "", "", watch,
					eventLogger.getTimestamp(eventLogger.getStartTime())));
			watch.start("IOEvent annotation Implicit Start");
			String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");
			String targetKey = START_PREFIX + ioEvent.key();
			List<String> topics = ioEventService.getTargetEventTopics(ioEvent, ioFlow);
			Message<Object> message = this.buildImplicitStartMessage(ioEvent, ioFlow, response, processName,
					uuid.toString(), targetKey, topics.isEmpty() ? "" : topics.get(0),
					eventLogger.getTimestamp(eventLogger.getStartTime()));
			kafkaTemplate.send(message);
			prepareAndDisplayEventLogger(eventLogger, uuid.toString(), ioEvent, processName, targetKey, response,
					watch);

		}

	}

	/**
	 * Method AfterReturning advice runs after a successful completion of a Implicit
	 * task with IOEvent annotation,
	 * 
	 * @param joinPoint    for the join point during the execution of the program,
	 * @param ioEvent      for ioevent annotation which include task information,
	 * @param returnObject for the returned object,
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws ParseException, JsonProcessingException {

		if (ioEventService.isImplicitTask(ioEvent)) {

			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			IOResponse<Object> response = ioEventService.getpayload(joinPoint, returnObject);
			StopWatch watch = new StopWatch();
			String target = "";
			IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);
			IOEventRecordInfo ioeventRecordInfoSource = IOEventContextHolder.getContext();
			if (!ioEventService.getSources(ioEvent).isEmpty()) {
				IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
				watch = ioeventRecordInfo.getWatch();
				Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
						response.getHeaders());
				ioeventRecordInfo.setWorkFlowName(
						ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
				Message<Object> message = this.buildMessage(ioEvent, ioFlow, response,
						ioeventRecordInfo.getWorkFlowName(), ioeventRecordInfo.getId(), END_PREFIX + ioEvent.key(), "",
						eventLogger.getTimestamp(eventLogger.getStartTime()),ioeventRecordInfo.getInstanceStartTime(), ioEventType, headers);

				kafkaTemplate.send(message);
				prepareAndDisplayEventLogger(eventLogger, ioEvent, ioeventRecordInfo, response,
						END_PREFIX + ioEvent.key(), ioEventType, watch);
				ioeventRecordInfoSource.setTargetName(END_PREFIX + ioEvent.key());
				createImpliciteEndEvent(ioEvent, ioFlow, ioeventRecordInfoSource, response, eventLogger);

			} else if (!ioEventService.getTargets(ioEvent).isEmpty()) {

				watch.start("IOEvent annotation Implicit TASK Aspect");
				ioeventRecordInfoSource.setWorkFlowName(ioEventService.getProcessName(ioEvent, ioFlow, ""));
				Map<String, Object> headers = ioEventService.prepareHeaders(null, response.getHeaders());
				for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
					String targetKey = ioEventService.getTargetKey(targetEvent);
					Message<Object> message = this.buildMessage(ioEvent, ioFlow, response,
							ioeventRecordInfoSource.getWorkFlowName(), ioeventRecordInfoSource.getId(), targetKey,
							targetEvent.topic(), eventLogger.getTimestamp(eventLogger.getStartTime()),
							ioeventRecordInfoSource.getInstanceStartTime(), ioEventType, headers);
					kafkaTemplate.send(message);
					target += targetKey + ",";
				}
				ioeventRecordInfoSource.setTargetName(START_PREFIX + ioEvent.key());
				prepareAndDisplayEventLogger(eventLogger, ioEvent, ioeventRecordInfoSource, response, target,
						ioEventType, watch);
			} else {

				watch.start("IOEvent annotation Implicit TASK Aspect");
				Map<String, Object> headers = ioEventService.prepareHeaders(null, response.getHeaders());
				Message<Object> message = this.buildMessage(ioEvent, ioFlow, response, ioFlow.name(),
						ioeventRecordInfoSource.getId(), END_PREFIX + ioEvent.key(), "",
						eventLogger.getTimestamp(eventLogger.getStartTime()),
						ioeventRecordInfoSource.getInstanceStartTime(), ioEventType, headers);

				kafkaTemplate.send(message);
				ioeventRecordInfoSource.setWorkFlowName(ioFlow.name());
				ioeventRecordInfoSource.setTargetName(START_PREFIX + ioEvent.key());
				prepareAndDisplayEventLogger(eventLogger, ioEvent, ioeventRecordInfoSource, response,
						END_PREFIX + ioEvent.key(), ioEventType, watch);
				ioeventRecordInfoSource.setTargetName(END_PREFIX + ioEvent.key());
				createImpliciteEndEvent(ioEvent, ioFlow, ioeventRecordInfoSource, response, eventLogger);
			}

		}

	}

	public void createImpliciteEndEvent(IOEvent ioEvent, IOFlow ioFlow, IOEventRecordInfo ioeventRecordInfo,
			IOResponse<Object> response, EventLogger eventLogger) throws ParseException, JsonProcessingException {
		StopWatch watch = new StopWatch();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Implicit End");
		Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
				response.getHeaders());
		Message<Object> message = this.buildMessage(ioEvent, ioFlow, response, ioeventRecordInfo.getWorkFlowName(),
				ioeventRecordInfo.getId(), "END", "", eventLogger.getTimestamp(eventLogger.getStartTime()),
				ioeventRecordInfo.getInstanceStartTime(), IOEventType.END, headers);

		kafkaTemplate.send(message);
		prepareAndDisplayEventLogger(eventLogger, ioEvent, response, watch, ioeventRecordInfo);
	}

	/**
	 * Method that build the event message of Implicit task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioflow            for ioflow annotation which include general
	 *                          information,
	 * @param payload           for the payload of the event,
	 * @param processName       for the process name
	 * @param uuid              for the correlation_id,
	 * @param targetEventName   for the target Event where the event will send ,
	 * @param targetTopic       for the name of the target topic ,
	 * @param startTime         for the start time of the event,
	 * @param instanceStartTime
	 * @return message type of Message,
	 */
	public Message<Object> buildMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload, String processName,
			String uuid, String targetEventName, String targetTopic, Long startTime, Long instanceStartTime,
			IOEventType ioEventType, Map<String, Object> headers) {

		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetTopic);
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		List<String> sourceEvents = ioEventService.getSourceNames(ioEvent);
		if (ioEventType.equals(IOEventType.END)) {
			sourceEvents = Arrays.asList(END_PREFIX + ioEvent.key());

		} else if (sourceEvents.isEmpty()) {
			sourceEvents.add(START_PREFIX + ioEvent.key());
		}
		return MessageBuilder.withPayload(payload.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, uuid).setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.STEP_NAME.toString(),
						ioEventType.equals(IOEventType.END) ? "END-EVENT" : ioEvent.key())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), sourceEvents)
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), targetEventName)
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), processName)
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), instanceStartTime).build();
	}

	public Message<Object> buildImplicitStartMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload,
			String processName, String uuid, String targetEventName, String targetTopic, Long startTime) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetTopic);
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(payload.getBody())
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, uuid).setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "START-EVENT")
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.START.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), new ArrayList<String>(Arrays.asList("Start")))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), targetEventName)
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), processName)
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), startTime).build();

	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger for the log info dto display,
	 * @param uuid        for the correlation_id,
	 * @param ioEvent     for ioevent annotation which include task information,
	 * @param ioflow      for ioflow annotation which include general information,
	 * @param payload     for the payload of the event,
	 * @param ioEventType for the IOEvent Type,
	 * @param watch       for capturing time,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent,
			IOEventRecordInfo ioEventRecordInfo, IOResponse<Object> payload, String targetName, IOEventType ioEventType,
			StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(ioEventRecordInfo.getId(), ioEventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioEventRecordInfo.getTargetName(), targetName, ioEventType.toString(), payload.getBody());
		eventLogger.stopEvent(watch.getTotalTimeMillis());
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
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent, IOResponse<Object> payload,
			StopWatch watch, IOEventRecordInfo ioeventRecordInfo) throws JsonProcessingException {

		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getTargetName(), "__", "End", payload.getBody());
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger for the log info dto display,
	 * @param uuid        for the correlation_id,
	 * @param ioEvent     for ioevent annotation which include task information,
	 * @param processName for the process name
	 * @param target      for the target where the event will send ,
	 * @param startTime   for the start time of the even
	 * @param payload     for the payload of the event,
	 * @param watch       for capturing time,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, String uuid, IOEvent ioEvent, String processName,
			String target, IOResponse<Object> payload, StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(uuid, processName, ioEvent.key(), null, target, "START", payload.getBody());
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
