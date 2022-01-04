package com.grizzlywave.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.IOFlow;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventHeaders;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect class for advice associated with a @IOEvent calls for Start task event
 * type
 */
@Slf4j
@Aspect
@Configuration
public class IOEventStartAspect {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WaveProperties waveProperties;
	@Autowired
	private IOEventService ioEventService;

	/**
	 * Method AfterReturning advice runs after a successful completion of a Start
	 * task with IOEvent annotation,
	 * 
	 * @param joinPoint    for the join point during the execution of the program,
	 * @param ioEvent      for ioevent annotation which include task information,
	 * @param returnObject for the returned object,
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws ParseException, JsonProcessingException {

		if (ioEventService.isStart(ioEvent)) {

			StopWatch watch = new StopWatch();
			EventLogger eventLogger = new EventLogger();

			eventLogger.startEventLog();
			watch.start("IOEvent annotation Start Aspect");

			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);

			UUID uuid = UUID.randomUUID();
			String target = "";
			Object payload = getpayload(joinPoint, returnObject);
			String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");

			for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
				Message<Object> message = this.buildStartMessage(ioEvent, ioFlow, payload, processName, uuid.toString(),
						targetEvent, eventLogger.getTimestamp(eventLogger.getStartTime()));
				kafkaTemplate.send(message);
				target += ioEventService.getTargetKey(targetEvent) + ",";
			}
			prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent, processName, target, payload, watch);
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
	 * Method that build the event message of Start task to be send in kafka topic,
	 * 
	 * @param ioEvent     for ioevent annotation which include task information,
	 * @param ioflow      for ioflow annotation which include general information,
	 * @param payload     for the payload of the event,
	 * @param processName for the process name
	 * @param uuid        for the correlation_id,
	 * @param targetEvent for the target Event where the event will send ,
	 * @param startTime   for the start time of the event,
	 * @return message type of Message,
	 */
	public Message<Object> buildStartMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload, String processName,
			String uuid, TargetEvent targetEvent, Long startTime) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetEvent.topic());
		String apiKey = ioEventService.getApiKey(waveProperties, ioFlow);
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, uuid).setHeader(IOEventHeaders.CORRELATION_ID.toString(), uuid)
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.START.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), new ArrayList<String>(Arrays.asList("Start")))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(),ioEventService.getTargetKey(targetEvent))
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
	 * @param processName for the process name
	 * @param target      for the target where the event will send ,
	 * @param payload     for the payload of the event,
	 * @param watch       for capturing time,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, UUID uuid, IOEvent ioEvent, String processName,
			String target, Object payload, StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(uuid.toString(), processName, ioEvent.key(), null, target, "Init", payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
