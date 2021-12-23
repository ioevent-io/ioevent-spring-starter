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
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

import lombok.extern.slf4j.Slf4j;
/**
 * Class Aspect which describe event transition task
 */
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


	/**
	 * Method AfterReturning advice runs after a successful completion of a transition task with IOEvent annotation,
	 * @param joinPoint for the point during the execution of the program,
	 * @param ioEvent for io event annotation which include task information,
	 * @param returnObject for the returned object,
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void transitionAspect(JoinPoint joinPoint, IOEvent ioEvent, Object returnObject) throws ParseException, JsonProcessingException  {
		
		
		if (isTransition(ioEvent)) {
			WaveRecordInfo waveRecordInfo= WaveContextHolder.getContext();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			StopWatch watch = waveRecordInfo.getWatch();
			String targets = "";
			IOEventType ioEventType =ioEventService.checkTaskType(ioEvent);
			Object payload = getpayload(joinPoint,returnObject); 
		
			if (ioEvent.gatewayTarget().target().length != 0) {

				if (ioEvent.gatewayTarget().parallel()) {
					ioEventType = IOEventType.GATEWAY_PARALLEL;
					targets = parallelEventSendProcess(ioEvent,payload,targets,waveRecordInfo,eventLogger);
					
				} else if (ioEvent.gatewayTarget().exclusive()) {
					ioEventType = IOEventType.GATEWAY_EXCLUSIVE;
					targets = exclusiveEventSendProcess(ioEvent,payload,targets,waveRecordInfo,eventLogger);
					
				}
			} else { 
				
		
					targets = simpleEventSendProcess(ioEvent,payload,targets,waveRecordInfo,eventLogger,ioEventType);
			}
			
			prepareAndDisplayEventLogger(eventLogger,waveRecordInfo,ioEvent,targets,watch,payload,ioEventType);
		}
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
	 * Method that build the start message of simple task,
	 * @param ioEvent for io event annotation which include task information,
	 * @param returnObject for the returned object,
	 * @param waveRecordInfo include information  about the task,
	 * @param eventLogger for the log info dto display,
	 * @param ioEventType for the event type,
	 * @param targets for the list of targets of the event separated by ",",
	 * @return  string format list of targets of the event separated by "," ,
	 */
	public String simpleEventSendProcess(IOEvent ioEvent, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo, EventLogger eventLogger, IOEventType ioEventType) throws ParseException {
		
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			
			Message<Object> message ;
			
			if (!StringUtils.isBlank(targetEvent.suffix())) {
				
				 message = this.buildSuffixMessage(ioEvent, returnObject, targetEvent,waveRecordInfo,waveRecordInfo.getStartTime(),ioEventType);
				 kafkaTemplate.send(message);

					targets += waveRecordInfo.getTargetName()+targetEvent.suffix();
			}
			else {
				 message = this.buildTransitionTaskMessage(ioEvent, returnObject, targetEvent,waveRecordInfo,waveRecordInfo.getStartTime(),ioEventType);
				 kafkaTemplate.send(message);

					targets += targetEvent.name() + ",";
			}
			
		}		return targets;
	}
	/**
	 * Method that build the start message of exclusive task,
	 * @param ioEvent for io event annotation which include task information,
	 * @param returnObject for the returned object,
	 * @param waveRecordInfo include information  about the task,
	 * @param eventLogger for the log info dto display,
	 * @param targets for the list of targets of the event separated by ",",
	 * @return  string format list of targets of the event separated by "," ,
	 */
	public String exclusiveEventSendProcess(IOEvent ioEvent, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo, EventLogger eventLogger) throws ParseException {
		
		IOEventResponse<Object> ioEventResponse = IOEventResponse.class.cast(returnObject);
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			if (ioEventResponse.getString().equals(targetEvent.name())) {
				Message<Object> message = this.buildTransitionGatewayExclusiveMessage(ioEvent, ioEventResponse.getBody(),
						targetEvent,waveRecordInfo,waveRecordInfo.getStartTime());
				kafkaTemplate.send(message);

				targets += targetEvent.name() + ",";
				log.info("sent to : {}", targetEvent.name());
			}

		}
		return targets;
	}
	/**
	 * Method that build the start message of parallel task,
	 * @param ioEvent for io event annotation which include task information,
	 * @param returnObject for the returned object,
	 * @param waveRecordInfo include information  about the task,
	 * @param eventLogger for the log info dto display,
	 * @param targets for the list of targets of the event separated by ",",
	 * @return  string format list of targets of the event separated by "," ,
	 */
	public String parallelEventSendProcess(IOEvent ioEvent, Object returnObject, String targets,
			WaveRecordInfo waveRecordInfo, EventLogger eventLogger) throws ParseException {
		for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
			Message<Object> message = this.buildTransitionGatewayParallelMessage(ioEvent, returnObject, targetEvent,waveRecordInfo,waveRecordInfo.getStartTime());
			kafkaTemplate.send(message);

			targets += targetEvent.name() + ",";
		}
		return targets;
	}
	/**
	 * Method that display logs after task completed ,
	 * @param eventLogger for the log info dto display,
	 * @param returnObject for the returned object,
	 * @param ioEvent for io event annotation which include task information,
	 * @param ioEventType for the event type,
	 * @param waveRecordInfo include information  about the task,
	 * @param target for the  target of the event,
	 * @param watch for capturing time,
	 */
	public void prepareAndDisplayEventLogger(EventLogger eventLogger, WaveRecordInfo waveRecordInfo, IOEvent ioEvent,
			String target, StopWatch watch,Object returnObject,IOEventType ioEventType) throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(waveRecordInfo.getId(),waveRecordInfo.getWorkFlowName(), ioEvent.name(), waveRecordInfo.getTargetName(), target, ioEventType.toString(),
				returnObject);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);		
	}
	/**
	 * Method check if task is a transition task,
	 * @param ioEvent for io event annotation which include task information,
	 */
	public boolean isTransition(IOEvent ioEvent) {
		return (StringUtils.isBlank(ioEvent.startEvent().key()) && StringUtils.isBlank(ioEvent.endEvent().key())&& !ioEventService.getSources(ioEvent).isEmpty()&&!ioEventService.getTargets(ioEvent).isEmpty());
	}
	/**
	 * Method that build the start message of Transition task,
	 * @param ioEvent for io event annotation which include task information,
	 * @param payload  for the payload of the event,
	 * @param waveRecordInfo include information  about the task,
	 * @param startTime for the start time of the event,
	 * @param ioEventType for the event type,
	 * @return  message type of Message,
	 */
	public Message<Object> buildTransitionTaskMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent, WaveRecordInfo waveRecordInfo, Long startTime, IOEventType ioEventType) {
		String topic = targetEvent.topic();
		if (StringUtils.isBlank(topic)) {
			topic = ioEvent.topic();

		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType",ioEventType.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}
	/**
	 * Method that build the message of Transition Gateway Parallel task,
	 * @param ioEvent for io event annotation which include task information,
	 * @param payload  for the payload of the event,
	 * @param waveRecordInfo include information  about the task,
	 * @param startTime for the start time of the event,
	 * @return  message type of Message,
	 */
	public Message<Object> buildTransitionGatewayParallelMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime) {
		String topic = targetEvent.topic();
		if (StringUtils.isBlank(topic)) {
			topic = ioEvent.topic();

		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}

	/**
	 * Method that build the message of Transition Gateway Exclusive task,
	 * @param ioEvent for io event annotation which include task information,
	 * @param payload  for the payload of the event,
	 * @param waveRecordInfo include information  about the task,
	 * @param startTime for the start time of the event,
	 * @return  message type of Message,
	 */
	public Message<Object> buildTransitionGatewayExclusiveMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime) {
		String topic = targetEvent.topic();
		if (StringUtils.isBlank(topic)) {
			topic = ioEvent.topic();

		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", IOEventType.GATEWAY_EXCLUSIVE.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}
	/**
	 * Method that build the Suffix message,
	 * @param ioEvent for io event annotation which include task information,
	 * @param payload  for the payload of the event,
	 * @param waveRecordInfo include information  about the task,
	 * @param startTime for the start time of the event,
	 * @param ioEventType for the event type,
	 * @return  message type of Message,
	 */
	public Message<Object> buildSuffixMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime, IOEventType ioEventType) {
		String topic = ioEventService.getSourceEventByName(ioEvent, waveRecordInfo.getTargetName()).topic();
		if (!StringUtils.isBlank(ioEvent.topic())) {
			topic = ioEvent.topic();

		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, waveRecordInfo.getId()).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", ioEventType.toString())
				.setHeader("source", waveRecordInfo.getTargetName())
				.setHeader("targetEvent", waveRecordInfo.getTargetName()+targetEvent.suffix()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}
}
