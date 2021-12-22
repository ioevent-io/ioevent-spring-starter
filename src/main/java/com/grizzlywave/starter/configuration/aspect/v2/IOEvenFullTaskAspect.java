package com.grizzlywave.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

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
import com.grizzlywave.starter.annotations.v2.IOFlow;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Configuration
public class IOEvenFullTaskAspect {
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WaveProperties waveProperties;
	@Autowired
	private IOEventService ioEventService;

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject)
			throws ParseException, JsonProcessingException {

		if (ioEventService.isFullTask(ioEvent)) {

			StopWatch watch = new StopWatch();
			EventLogger eventLogger = new EventLogger();

			eventLogger.startEventLog();
			watch.start("IOEvent annotation FULL TASK Aspect");
			
			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);

			UUID uuid = UUID.randomUUID();
			Object payload = getpayload(joinPoint, returnObject);

			Message<Object> message = this.buildStartMessage(ioEvent, payload, uuid.toString(),ioFlow,
					eventLogger.getTimestamp(eventLogger.getStartTime()));
			kafkaTemplate.send(message);

			prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent,ioFlow, payload, watch);
		}

	}



	public Object getpayload(JoinPoint joinPoint, Object returnObject) {
		if (returnObject == null) {
			return joinPoint.getArgs()[0];

		}
		return returnObject;
	}

	public Message<Object> buildStartMessage(IOEvent ioEvent, Object payload, String uuid, IOFlow ioFlow, Long startTime) {
		String topic = ioEvent.topic();
		if (StringUtils.isBlank(topic)) {
			topic = ioFlow.topic();
		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, uuid).setHeader("Correlation_id", uuid)
				.setHeader("StepName", ioEvent.name()).setHeader("EventType", IOEventType.FULLTASK.toString())
				.setHeader("source", new ArrayList<String>(Arrays.asList("Start")))
				.setHeader("targetEvent", new ArrayList<String>(Arrays.asList("END"))).setHeader("Process_Name",ioFlow.name())
				.setHeader("Start Time", startTime).build();
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, UUID uuid, IOEvent ioEvent, IOFlow ioFlow,Object payload,
			StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(uuid.toString(), ioFlow.name(), ioEvent.name(), "START", "END",
				IOEventType.FULLTASK.toString(), payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
