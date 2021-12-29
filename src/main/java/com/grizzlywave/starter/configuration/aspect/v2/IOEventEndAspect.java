package com.grizzlywave.starter.configuration.aspect.v2;

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
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventHeaders;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect class for advice associated with a @IOEvent calls for End task event
 * type
 */
@Slf4j
@Aspect
@Configuration
public class IOEventEndAspect {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private WaveProperties waveProperties;

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
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws JsonProcessingException {
		if (ioEventService.isEnd(ioEvent)) {
			WaveRecordInfo waveRecordInfo = WaveContextHolder.getContext();
			StopWatch watch = waveRecordInfo.getWatch();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			waveRecordInfo
					.setWorkFlowName(ioEventService.getProcessName(ioEvent, ioFlow, waveRecordInfo.getWorkFlowName()));
			Object payload = getpayload(joinPoint, returnObject);
			String target = "END";
			Message<Object> message = this.buildEventMessage(ioEvent, ioFlow, payload, target, waveRecordInfo,
					waveRecordInfo.getStartTime());
			kafkaTemplate.send(message);
			prepareAndDisplayEventLogger(eventLogger, ioEvent, payload, watch, waveRecordInfo);
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
	 * Method that build the event message of End task to be send in kafka topic,
	 * 
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param ioflow         for ioflow annotation which include general
	 *                       information,
	 * @param payload        for the payload of the event,
	 * @param processName    for the process name
	 * @param targetEvent    for the target Event where the event will send ,
	 * @param waveRecordInfo for the record information from the consumed event,
	 * @param startTime      for the start time of the event,
	 * @return message type of Message,
	 */
	public Message<Object> buildEventMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload, String targetEvent,
			WaveRecordInfo waveRecordInfo, Long startTime) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, "");
		String apiKey = ioEventService.getApiKey(waveProperties, ioFlow);
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), waveRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), targetEvent)
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), waveRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.END.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), ioEventService.getSourceNames(ioEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.name())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime).build();
	}

	/**
	 * Method that display logs after task completed ,
	 * 
	 * @param eventLogger    for the log info dto display,
	 * @param ioEvent        for ioevent annotation which include task information,
	 * @param payload        for the payload of the event,
	 * @param watch          for capturing time,
	 * @param waveRecordInfo for the record information from the consumed event,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent, Object payload, StopWatch watch,
			WaveRecordInfo waveRecordInfo) throws JsonProcessingException {

		watch.stop();
		eventLogger.loggerSetting(waveRecordInfo.getId(), waveRecordInfo.getWorkFlowName(), ioEvent.name(),
				waveRecordInfo.getTargetName(), "__", "End", payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
