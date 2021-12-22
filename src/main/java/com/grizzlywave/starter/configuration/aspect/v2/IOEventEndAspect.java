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
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

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

	

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void iOEventAnnotationAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject) throws JsonProcessingException  {
		if (ioEventService.isEnd(ioEvent)) {
			WaveRecordInfo waveRecordInfo= WaveContextHolder.getContext();
			StopWatch watch = waveRecordInfo.getWatch();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			waveRecordInfo.setWorkFlowName(ioEventService.getProcessName(ioEvent,ioFlow,waveRecordInfo.getWorkFlowName()));
			Object payload = getpayload(joinPoint,returnObject); 
			String target = "END";
			Message<Object> message = this.buildEventMessage(ioEvent,ioFlow, payload, target,
					waveRecordInfo, waveRecordInfo.getStartTime());
			kafkaTemplate.send(message);
			prepareAndDisplayEventLogger(eventLogger,ioEvent, payload, watch,waveRecordInfo);
		}
	}
	
	public Object getpayload(JoinPoint joinPoint, Object returnObject) {
		if (returnObject==null) {
			return joinPoint.getArgs()[0];

		}		return returnObject;
	}
	public Message<Object> buildEventMessage(IOEvent ioEvent, IOFlow ioFlow, Object payload, String targetEvent,
			WaveRecordInfo waveRecordInfo, Long startTime) {
		String topicName= ioEventService.getTargetTopicName(ioEvent,ioFlow,"");

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId())
				.setHeader("Process_Name", waveRecordInfo.getWorkFlowName()).setHeader("targetEvent", targetEvent)
				.setHeader("Correlation_id", waveRecordInfo.getId()).setHeader("EventType", IOEventType.END.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent)).setHeader("StepName", ioEvent.name())
				.setHeader("Start Time", startTime).build();
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEvent ioEvent,
			Object payload, StopWatch watch,WaveRecordInfo waveRecordInfo) throws JsonProcessingException {

		watch.stop();
		eventLogger.loggerSetting(waveRecordInfo.getId(), waveRecordInfo.getWorkFlowName(), ioEvent.name(),
				waveRecordInfo.getTargetName(), "__", "End", payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
