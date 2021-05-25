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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

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

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void receive(JoinPoint joinPoint, IOEvent ioEvent, Object object) throws Throwable {
		if (ioEvent.startEvent().key().equals("") && (ioEvent.endEvent().key().equals(""))) {
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			StopWatch watch = new StopWatch();
			watch.start("IOEvent annotation Transition Aspect");
			String target = "";
			for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
				Message<Object> message = this.buildStartMessage(ioEvent, object, targetEvent);
				kafkaTemplate.send(message);
				target += targetEvent.name() + ",";
			}
			watch.stop();
			eventLogger.setting(null,"", ioEvent.name(), ioEventService.getSourceNames(ioEvent), target,
					"Transition", object);
			eventLogger.stopEvent(watch.getTotalTimeMillis());
			String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
			log.info(jsonObject);
		}
	}

	private Message<Object> buildStartMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent) {
		return MessageBuilder.withPayload(payload)
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + targetEvent.topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).build();
	}

}
