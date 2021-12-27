package com.grizzlywave.starter.configuration.aspect.v2;

import java.text.ParseException;
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
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Configuration
public class IOEvenImplicitTaskAspect {
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

		if (ioEventService.isImplicitTask(ioEvent)) {

			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			Object payload = getpayload(joinPoint, returnObject);
			StopWatch watch = new StopWatch();
			UUID uuid = UUID.randomUUID();
			String target = "";
			IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);

			if (!ioEventService.getSources(ioEvent).isEmpty()) {
				WaveRecordInfo waveRecordInfo = WaveContextHolder.getContext();
				watch = waveRecordInfo.getWatch();
				waveRecordInfo.setWorkFlowName(
						ioEventService.getProcessName(ioEvent, ioFlow, waveRecordInfo.getWorkFlowName()));
				Message<Object> message = this.buildMessage(ioEvent, ioFlow, payload, waveRecordInfo.getWorkFlowName(),
						waveRecordInfo.getId(), "", "", eventLogger.getTimestamp(eventLogger.getStartTime()),
						ioEventType);

				kafkaTemplate.send(message);
				prepareAndDisplayEventLogger(eventLogger, ioEvent, payload, watch, waveRecordInfo);
			} else if (!ioEventService.getTargets(ioEvent).isEmpty()) {

				watch.start("IOEvent annotation Implicit TASK Aspect");
				String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");
				for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
					Message<Object> message = this.buildMessage(ioEvent, ioFlow, payload, processName, uuid.toString(),
							targetEvent.name(), targetEvent.topic(),
							eventLogger.getTimestamp(eventLogger.getStartTime()), ioEventType);
					kafkaTemplate.send(message);
					target += targetEvent.name() + ",";
				}
				prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent, processName, target, payload, watch);
			} else {

				watch.start("IOEvent annotation Implicit TASK Aspect");
				Message<Object> message = this.buildMessage(ioEvent, ioFlow, payload, ioFlow.name(), uuid.toString(),
						"", "", eventLogger.getTimestamp(eventLogger.getStartTime()), ioEventType);

				kafkaTemplate.send(message);

				prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent, ioFlow, payload, watch);
			}

		}

	}

	public Object getpayload(JoinPoint joinPoint, Object returnObject) {
		if (returnObject == null) {
			return joinPoint.getArgs()[0];

		}
		return returnObject;
	}

	public Message<Object> buildMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload, String processName, String uuid,
			String targetEventName, String targetTopic, Long startTime, IOEventType ioEventType) {

		String topicName = ioEventService.getTargetTopicName(ioEvent, ioFlow, targetTopic);

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, uuid).setHeader("Correlation_id", uuid)
				.setHeader("StepName", ioEvent.name()).setHeader("EventType", ioEventType.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent)).setHeader("targetEvent", targetEventName)
				.setHeader("Process_Name", processName).setHeader("Start Time", startTime).build();
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, UUID uuid, IOEvent ioEvent, IOFlow ioFlow,
			Object payload, StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(uuid.toString(), ioFlow.name(), ioEvent.name(), "START", "END",
				IOEventType.IMPLICITTASK.toString(), payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent, Object payload, StopWatch watch,
			WaveRecordInfo waveRecordInfo) throws JsonProcessingException {

		watch.stop();
		eventLogger.loggerSetting(waveRecordInfo.getId(), waveRecordInfo.getWorkFlowName(), ioEvent.name(),
				waveRecordInfo.getTargetName(), "__", "End", payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, UUID uuid, IOEvent ioEvent, String processName,
			String target, Object payload, StopWatch watch) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(uuid.toString(), processName, ioEvent.name(), null, target, "START", payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
