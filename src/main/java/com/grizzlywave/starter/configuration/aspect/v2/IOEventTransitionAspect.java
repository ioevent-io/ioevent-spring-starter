package com.grizzlywave.starter.configuration.aspect.v2;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
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
import com.grizzlywave.starter.annotations.v2.IOEventResponse;
import com.grizzlywave.starter.annotations.v2.SendRecordInfo;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Configuration
public class IOEventTransitionAspect {

	private WaveRecordInfo waveRecordInfo= null;
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WaveProperties waveProperties;

	@Autowired
	private IOEventService ioEventService;

	
	@Around(value = "@annotation(audit)")
	public Object SendRecordInfoAspect(ProceedingJoinPoint pjp,SendRecordInfo audit) throws Throwable {
		this.waveRecordInfo =WaveRecordInfo.class.cast(pjp.getArgs()[0]);
		Object obj = pjp.proceed();
		return obj;
	}

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void transitionAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject) throws Throwable {
		
		
		if (isTransition(ioEvent)) {
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			StopWatch watch = new StopWatch();
			watch.start("IOEvent annotation Transition Aspect");
			String target = "";
			if (ioEvent.gatewayTarget().target().length != 0) {
				if (ioEvent.gatewayTarget().parallel()) {
					for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
						Message<Object> message = this.buildStartMessage(ioEvent, returnObject, targetEvent,this.waveRecordInfo,eventLogger.getTimestamp(eventLogger.getStartTime()));
						kafkaTemplate.send(message);
						target += targetEvent.name() + ",";
					}
				} else if (ioEvent.gatewayTarget().exclusive()) {
					IOEventResponse<Object> ioEventResponse = IOEventResponse.class.cast(returnObject);
					for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
						if (ioEventResponse.getString().equals(targetEvent.name())) {
							Message<Object> message = this.buildStartMessage(ioEvent, ioEventResponse.getBody(),
									targetEvent,this.waveRecordInfo,eventLogger.getTimestamp(eventLogger.getStartTime()));
							kafkaTemplate.send(message);
							target += targetEvent.name() + ",";
							log.info("sent to :"+targetEvent.name());
						}

					}

				}
			} else {
			
				for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
					Message<Object> message ;
					if (!targetEvent.suffix().equals("")) {
						
						 message = this.buildSuffixMessage(ioEvent, returnObject, targetEvent,this.waveRecordInfo,eventLogger.getTimestamp(eventLogger.getStartTime()));
						 kafkaTemplate.send(message);
							target += waveRecordInfo.getTargetName()+targetEvent.suffix();
					}
					else {
						 message = this.buildStartMessage(ioEvent, returnObject, targetEvent,this.waveRecordInfo,eventLogger.getTimestamp(eventLogger.getStartTime()));
						 kafkaTemplate.send(message);
							target += targetEvent.name() + ",";
					}
					
				}
			}
			
			prepareAndDisplayEventLogger(eventLogger,waveRecordInfo,ioEvent,target,joinPoint,watch,returnObject);
		}
	}

	private void prepareAndDisplayEventLogger(EventLogger eventLogger, WaveRecordInfo waveRecordInfo2, IOEvent ioEvent,
			String target, JoinPoint joinPoint, StopWatch watch,Object returnObject) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(this.waveRecordInfo.getId(), this.waveRecordInfo.getWorkFlowName(), ioEvent.name(), this.waveRecordInfo.getTargetName(), target, "Transition",
				returnObject);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);		
	}

	private boolean isTransition(IOEvent ioEvent) {
		return (ioEvent.startEvent().key().isEmpty() && ioEvent.endEvent().key().isEmpty());
	}

	private Message<Object> buildStartMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime) {
		String topic = targetEvent.topic();
		if (topic.equals("")) {
			topic = ioEvent.topic();

		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", IOEventType.TRANSITION.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}

	
	private Message<Object> buildSuffixMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime) {
		String topic = ioEventService.getSourceEventByName(ioEvent, waveRecordInfo.getTargetName()).topic();
		if (!ioEvent.topic().equals("")) {
			topic = ioEvent.topic();

		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", IOEventType.TRANSITION.toString())
				.setHeader("source", waveRecordInfo.getTargetName())
				.setHeader("targetEvent", waveRecordInfo.getTargetName()+targetEvent.suffix()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}
}
