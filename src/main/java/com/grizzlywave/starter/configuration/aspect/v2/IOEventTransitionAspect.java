package com.grizzlywave.starter.configuration.aspect.v2;

import java.text.ParseException;

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
import com.grizzlywave.starter.annotations.v2.IOEventResponse;
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
public class IOEventTransitionAspect {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WaveProperties waveProperties;

	@Autowired
	private IOEventService ioEventService;

	
	

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void transitionAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject) throws ParseException, JsonProcessingException  {
		
		
		if (ioEventService.isTransition(ioEvent)) {
			WaveRecordInfo waveRecordInfo= WaveContextHolder.getContext();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			StopWatch watch = waveRecordInfo.getWatch();
			IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
			waveRecordInfo.setWorkFlowName(ioEventService.getProcessName(ioEvent,ioFlow,waveRecordInfo.getWorkFlowName()));
			String targets = "";
			IOEventType ioEventType =ioEventService.checkTaskType(ioEvent);
			Object payload = getpayload(joinPoint,returnObject); 
		
			if (ioEvent.gatewayTarget().target().length != 0) {

				if (ioEvent.gatewayTarget().parallel()) {
					ioEventType = IOEventType.GATEWAY_PARALLEL;
					targets = parallelEventSendProcess(ioEvent,ioFlow,payload,targets,waveRecordInfo,eventLogger);
					
				} else if (ioEvent.gatewayTarget().exclusive()) {
					ioEventType = IOEventType.GATEWAY_EXCLUSIVE;
					targets = exclusiveEventSendProcess(ioEvent,ioFlow,payload,targets,waveRecordInfo,eventLogger);
					
				}
			} else { 
				
		
					targets = simpleEventSendProcess(ioEvent,ioFlow,payload,targets,waveRecordInfo,eventLogger,ioEventType);
			}
			
			prepareAndDisplayEventLogger(eventLogger,waveRecordInfo,ioEvent,targets,watch,payload,ioEventType);
		}
	}

	
	public Object getpayload(JoinPoint joinPoint, Object returnObject) {
		if (returnObject==null) {
			return joinPoint.getArgs()[0];

		}		return returnObject;
	}


	public String simpleEventSendProcess(IOEvent ioEvent,IOFlow ioFlow, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo, EventLogger eventLogger, IOEventType ioEventType) throws ParseException {
		
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			
			Message<Object> message ;
			
			if (!StringUtils.isBlank(targetEvent.suffix())) {
				
				 message = this.buildSuffixMessage(ioEvent,ioFlow, returnObject, targetEvent,waveRecordInfo,waveRecordInfo.getStartTime(),ioEventType);
				 kafkaTemplate.send(message);

					targets += waveRecordInfo.getTargetName()+targetEvent.suffix();
			}
			else {
				 message = this.buildTransitionTaskMessage(ioEvent,ioFlow, returnObject, targetEvent,waveRecordInfo,waveRecordInfo.getStartTime(),ioEventType);
				 kafkaTemplate.send(message);

					targets += targetEvent.name() + ",";
			}
			
		}		return targets;
	}

	public String exclusiveEventSendProcess(IOEvent ioEvent,IOFlow ioFlow, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo, EventLogger eventLogger) throws ParseException {
		
		IOEventResponse<Object> ioEventResponse = IOEventResponse.class.cast(returnObject);
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			if (ioEventResponse.getString().equals(targetEvent.name())) {
				Message<Object> message = this.buildTransitionGatewayExclusiveMessage(ioEvent,ioFlow, ioEventResponse.getBody(),
						targetEvent,waveRecordInfo,waveRecordInfo.getStartTime());
				kafkaTemplate.send(message);

				targets += targetEvent.name() + ",";
				log.info("sent to : {}", targetEvent.name());
			}

		}
		return targets;
	}

	public String parallelEventSendProcess(IOEvent ioEvent,IOFlow ioFlow, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo, EventLogger eventLogger) {
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			Message<Object> message = this.buildTransitionGatewayParallelMessage(ioEvent,ioFlow, returnObject, targetEvent,waveRecordInfo,waveRecordInfo.getStartTime());
			kafkaTemplate.send(message);

			targets += targetEvent.name() + ",";
		}
		return targets;
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, WaveRecordInfo waveRecordInfo, IOEvent ioEvent,
			String target, StopWatch watch,Object returnObject,IOEventType ioEventType) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(waveRecordInfo.getId(),waveRecordInfo.getWorkFlowName(), ioEvent.name(), waveRecordInfo.getTargetName(), target, ioEventType.toString(),
				returnObject);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);		
	}



	public Message<Object> buildTransitionTaskMessage(IOEvent ioEvent,IOFlow ioFlow, Object payload, TargetEvent targetEvent, WaveRecordInfo waveRecordInfo, Long startTime, IOEventType ioEventType) {
		String topicName= ioEventService.getTargetTopicName(ioEvent,ioFlow,targetEvent.topic());

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType",ioEventType.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}
	public Message<Object> buildTransitionGatewayParallelMessage(IOEvent ioEvent ,IOFlow ioFlow , Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime) {
		String topicName= ioEventService.getTargetTopicName(ioEvent,ioFlow,targetEvent.topic());
		
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}


	public Message<Object> buildTransitionGatewayExclusiveMessage(IOEvent ioEvent,IOFlow ioFlow, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime) {
		String topicName= ioEventService.getTargetTopicName(ioEvent,ioFlow,targetEvent.topic());

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", IOEventType.GATEWAY_EXCLUSIVE.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}
	
	public Message<Object> buildSuffixMessage(IOEvent ioEvent,IOFlow ioFlow, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime, IOEventType ioEventType) {
		String sourcetopic = ioEventService.getSourceEventByName(ioEvent, waveRecordInfo.getTargetName()).topic();
		String topicName= ioEventService.getTargetTopicName(ioEvent,ioFlow,sourcetopic);

		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", ioEventType.toString())
				.setHeader("source", waveRecordInfo.getTargetName())
				.setHeader("targetEvent", waveRecordInfo.getTargetName()+targetEvent.suffix()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}
}
