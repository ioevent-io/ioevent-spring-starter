package com.grizzlywave.starter.configuration.aspect.v2;

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
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

import lombok.extern.slf4j.Slf4j;
/**
 * Class Aspect which describe event end task
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
	 * Method AfterReturning advice runs after a successful completion of an end task with IOEvent annotation,
	 * @param joinPoint for the point during the execution of the program,
	 * @param ioEvent for io event annotation which include task information,
	 * @param returnObject for the returned object,
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject) throws JsonProcessingException  {
		if (isEnd(ioEvent)) {
			WaveRecordInfo waveRecordInfo= WaveContextHolder.getContext();
			StopWatch watch = waveRecordInfo.getWatch();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			String workflow = ioEvent.endEvent().key();
			Object payload = getpayload(joinPoint,returnObject); 
			String target = "END";
			Message<Object> message = this.buildEventMessage(ioEvent, payload, target,
					waveRecordInfo, waveRecordInfo.getStartTime());
			kafkaTemplate.send(message);
			prepareAndDisplayEventLogger(eventLogger, workflow, ioEvent, payload, watch,waveRecordInfo);
		}
	}
	/**
	 * Method check if task is an end task,
	 * @param ioEvent for io event annotation which include task information,
	 * @return  boolean
	 */
	public boolean isEnd(IOEvent ioEvent) {
		return ((ioEventService.getTargets(ioEvent).isEmpty() || !StringUtils.isBlank(ioEvent.endEvent().key()))
				&& (!ioEventService.getSources(ioEvent).isEmpty()));
	}
	/**
	 * Method that returns event payload,
	 * @param joinPoint for the point during the execution of the program,
	 * @param returnObject for the returned object,
	 * @return  An object of type Object,
	 */
	public Object getpayload(JoinPoint joinPoint, Object returnObject) {
		if (returnObject==null) {
			return joinPoint.getArgs()[0];

		}		return returnObject;
	}
	/**
	 * Method that build the end message,
	 * @param ioEvent for io event annotation which include task information,
	 * @param payload  for the payload of the event,
	 * @param waveRecordInfo include information  about the task,
	 * @param startTime for the start time of the event,
	 * @return  message type of Message,
	 */
	public Message<Object> buildEventMessage(IOEvent ioEvent, Object payload, String targetEvent,
			WaveRecordInfo waveRecordInfo, Long startTime) {
		String topic = ioEvent.topic();
		if (StringUtils.isBlank(topic)) {
			topic = ioEvent.topic();
		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId())
				.setHeader("Process_Name", ioEvent.endEvent().key()).setHeader("targetEvent", targetEvent)
				.setHeader("Correlation_id", waveRecordInfo.getId()).setHeader("EventType", IOEventType.END.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent)).setHeader("StepName", ioEvent.name())
				.setHeader("Start Time", startTime).build();
	}
	/**
	 * Method that display logs after task completed ,
	 * @param eventLogger for the log info dto display,
	 * @param workflow for the name of the process,
	 * @param ioEvent for io event annotation which include task information,
	 * @param payload  for the payload of the event,
	 * @param waveRecordInfo include information  about the task,
	 * @param watch for capturing time,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, String workflow, IOEvent ioEvent,
			Object payload, StopWatch watch,WaveRecordInfo waveRecordInfo) throws JsonProcessingException {

		watch.stop();
		eventLogger.loggerSetting(waveRecordInfo.getId(), workflow, ioEvent.name(),
				waveRecordInfo.getTargetName(), "__", "End", payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
