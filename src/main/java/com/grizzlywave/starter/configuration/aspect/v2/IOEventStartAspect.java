package com.grizzlywave.starter.configuration.aspect.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

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
	
	@Around(value = "@annotation(anno)", argNames = "jp, anno") //
	public Object iOEventAnnotationAspect(ProceedingJoinPoint joinPoint, IOEvent ioEvent) throws Throwable {
		Object obj = joinPoint.proceed();
		if (!ioEvent.startEvent().key().equals("")) {
			StopWatch watch = new StopWatch();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			watch.start("IOEvent annotation Start Aspect");
			UUID uuid = UUID.randomUUID();
			String target ="";
			for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
				Message<Object> message = this.buildStartMessage(ioEvent, joinPoint.getArgs()[0],uuid.toString(),targetEvent,eventLogger.getTimestamp(eventLogger.getStartTime()));
				kafkaTemplate.send(message);
				target+=targetEvent.name()+",";
			}
			watch.stop();
			eventLogger.setting(uuid.toString(), ioEvent.startEvent().key(), ioEvent.name(),null,target, "Init",
					joinPoint.getArgs()[0].toString()); 
			eventLogger.stopEvent(watch.getTotalTimeMillis());
			String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
			log.info(jsonObject);
		} 
		return obj;
	}


	private Message<Object> buildStartMessage(IOEvent ioEvent, Object payload, String uuid, TargetEvent targetEvent, Long startTime) {
		return MessageBuilder.withPayload(payload)
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + targetEvent.topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("Correlation_id",uuid).setHeader("StepName", ioEvent.name())
				.setHeader("EventType", IOEventType.START.toString())
				.setHeader("source",new ArrayList<String>(Arrays.asList("Start")))
				.setHeader("targetEvent", targetEvent.name())
				.setHeader("Process_Name", ioEvent.startEvent().key()).setHeader("Start Time", startTime).build();
	}

}
