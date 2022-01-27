package com.ioevent.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.Map;
import java.util.UUID;

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
			IOResponse<Object> response =ioEventService.getpayload(joinPoint, returnObject);
			StopWatch watch = new StopWatch();
			UUID uuid = UUID.randomUUID();
			String target = "";
			IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);

			if (!ioEventService.getSources(ioEvent).isEmpty()) {
				IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
				watch = ioeventRecordInfo.getWatch();
				Map<String, Object> headers=ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),response.getHeaders());
				ioeventRecordInfo.setWorkFlowName(
						ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
				Message<Object> message = this.buildMessage(ioEvent, ioFlow, response, ioeventRecordInfo.getWorkFlowName(),
						ioeventRecordInfo.getId(), "", "", eventLogger.getTimestamp(eventLogger.getStartTime()),
						ioEventType,headers);

				kafkaTemplate.send(message);
				prepareAndDisplayEventLogger(eventLogger, ioEvent, response, watch, ioeventRecordInfo);
			} else if (!ioEventService.getTargets(ioEvent).isEmpty()) {

				watch.start("IOEvent annotation Implicit TASK Aspect");
				String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");
				Map<String, Object> headers=ioEventService.prepareHeaders(null,response.getHeaders());
				for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
				String targetKey=	ioEventService.getTargetKey(targetEvent) ;
					Message<Object> message = this.buildMessage(ioEvent, ioFlow, response, processName, uuid.toString(),
							targetKey, targetEvent.topic(),
							eventLogger.getTimestamp(eventLogger.getStartTime()), ioEventType,headers);
					kafkaTemplate.send(message);
					target += targetKey + ",";
				}
				prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent, processName, target, response, watch);
			} else {

				watch.start("IOEvent annotation Implicit TASK Aspect");
				Map<String, Object> headers=ioEventService.prepareHeaders(null,response.getHeaders());
				Message<Object> message = this.buildMessage(ioEvent, ioFlow, response, ioFlow.name(), uuid.toString(),
						"", "", eventLogger.getTimestamp(eventLogger.getStartTime()), ioEventType,headers);

				kafkaTemplate.send(message);

				prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent, ioFlow, response, watch);
			}

		}

	}

	/**
	 * Method that build the event message of Implicit task to be send in kafka
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
	public Message<Object> buildMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload, String processName, String uuid,
			String targetEventName, String targetTopic, Long startTime, IOEventType ioEventType,Map<String, Object> headers) {

		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetTopic);
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(payload.getBody()).copyHeaders(headers).setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, uuid).setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), ioEventService.getSourceNames(ioEvent))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), targetEventName)
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), processName)
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime).build();
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger for the log info dto display,
	 * @param uuid        for the correlation_id,
	 * @param ioEvent     for ioevent annotation which include task information,
	 * @param ioflow      for ioflow annotation which include general information,
	 * @param payload     for the payload of the event,
	 * @param watch       for capturing time,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, UUID uuid, IOEvent ioEvent, IOFlow ioFlow,
			IOResponse<Object> payload, StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(uuid.toString(), ioFlow.name(), ioEvent.key(), "START", "END",
				IOEventType.IMPLICITTASK.toString(), payload.getBody());
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger    for the log info dto display,
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param payload        for the payload of the event,
	 * @param watch          for capturing time,
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent, IOResponse<Object> payload, StopWatch watch,
			IOEventRecordInfo ioeventRecordInfo) throws JsonProcessingException {

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
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, UUID uuid, IOEvent ioEvent, String processName,
			String target, IOResponse<Object> payload, StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(uuid.toString(), processName, ioEvent.key(), null, target, "START", payload.getBody());
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
