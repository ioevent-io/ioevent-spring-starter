package com.ioevent.starter.configuration.aspect.v2;

import java.util.Map;

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
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
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
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws JsonProcessingException {
		if (ioEventService.isEnd(ioEvent)) {
			IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
			Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
					ioEventService.getpayload(joinPoint, returnObject).getHeaders());

			StopWatch watch = ioeventRecordInfo.getWatch();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			ioeventRecordInfo.setWorkFlowName(
					ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
			IOResponse<Object> payload = ioEventService.getpayload(joinPoint, returnObject);
			String target = "END";
			Message<Object> message = this.buildEventMessage(ioEvent, ioFlow, payload, target, ioeventRecordInfo,
					ioeventRecordInfo.getStartTime(), headers);
			kafkaTemplate.send(message);
			prepareAndDisplayEventLogger(eventLogger, ioEvent, payload.getBody(), watch, ioeventRecordInfo);
		}
	}

	/**
	 * Method that build the event message of End task to be send in kafka topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioflow            for ioflow annotation which include general
	 *                          information,
	 * @param payload           for the payload of the event,
	 * @param processName       for the process name
	 * @param targetEvent       for the target Event where the event will send ,
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @param startTime         for the start time of the event,
	 * @param headers
	 * @return message type of Message,
	 */
	public Message<Object> buildEventMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload,
			String targetEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime, Map<String, Object> headers) {
		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, "");
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		return MessageBuilder.withPayload(payload.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), targetEvent)
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.END.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), ioEventService.getSourceNames(ioEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.build();
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
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent, Object payload, StopWatch watch,
			IOEventRecordInfo ioeventRecordInfo) throws JsonProcessingException {

		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getTargetName(), "__", "End", payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
