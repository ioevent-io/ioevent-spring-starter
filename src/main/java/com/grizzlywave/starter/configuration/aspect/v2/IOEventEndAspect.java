package com.grizzlywave.starter.configuration.aspect.v2;

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
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SendRecordInfo;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

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
	private WaveRecordInfo waveRecordInfo;

	@Before(value = "@annotation(audit)")
	public void methodHandlerAspect(JoinPoint pjp, SendRecordInfo audit) throws Throwable {
		this.waveRecordInfo = WaveRecordInfo.class.cast(pjp.getArgs()[0]);
	}

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object object) throws Throwable {
		if (!ioEvent.endEvent().key().isEmpty()) {
			StopWatch watch = new StopWatch();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			watch.start("IOEvent End annotation Aspect");
			String workflow = ioEvent.endEvent().key();
			String target = "END";
			Message<Object> message = this.buildEventMessage(ioEvent, joinPoint.getArgs()[0], target,
					this.waveRecordInfo, eventLogger.getTimestamp(eventLogger.getStartTime()));
			kafkaTemplate.send(message);

			prepareAndDisplayEventLogger(eventLogger, workflow, ioEvent, joinPoint, watch);
		}
	}

	
	public Message<Object> buildEventMessage(IOEvent ioEvent, Object payload, String targetEvent,
			WaveRecordInfo waveRecordInfo, Long startTime) {
		String topic = ioEvent.topic();
		if (topic.isEmpty()) {
			topic = ioEvent.topic();
		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("Process_Name", ioEvent.endEvent().key()).setHeader("targetEvent", targetEvent)
				.setHeader("Correlation_id", waveRecordInfo.getId()).setHeader("EventType", IOEventType.END.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent)).setHeader("StepName", ioEvent.name())
				.setHeader("Start Time", startTime).build();
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, String workflow, IOEvent ioEvent,
			JoinPoint joinPoint, StopWatch watch) throws JsonProcessingException {

		watch.stop();
		eventLogger.loggerSetting(this.waveRecordInfo.getId(), workflow, ioEvent.name(),
				this.waveRecordInfo.getTargetName(), "__", "End", joinPoint.getArgs()[0]);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
