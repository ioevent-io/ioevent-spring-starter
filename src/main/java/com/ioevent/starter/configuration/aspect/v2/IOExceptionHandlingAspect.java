package com.ioevent.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
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
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.OutputEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect class for advice associated with a @IOEvent calls for Exception task
 * event type
 */
@Slf4j
@Aspect
@Configuration
public class IOExceptionHandlingAspect {
	
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private IOEventProperties iOEventProperties;

	@Autowired
	private IOEventService ioEventService;
	
	/**
	 * Method AfterThrowing advice is executed after a join point does not 
	 * complete normally and end up throwing an exception.
	 * 
	 * @param joinPoint    for the join point during the execution of the program,
	 * @param ioEvent      for ioevent annotation which include task information,
	 * @param throwable	   for throwed exception during the execution,
	 * @throws ParseException
	 * @throws JsonProcessingException
	 */
	@AfterThrowing(value = "@annotation(anno)", argNames = "jp, anno,ex", throwing = "ex")
	public void throwingExceptionAspect(JoinPoint joinPoint, IOEvent ioEvent, Throwable throwable)
			throws ParseException, JsonProcessingException {
		//Error Boundry Event
		if(ioEvent.exception().exception().length!=0 && hasTobeHandled(ioEvent.exception().exception(), throwable)) {
				
		//Capture annotation information to complete the workflow 
		IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
		
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		StopWatch watch = ioeventRecordInfo.getWatch();

		IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
		
		ioeventRecordInfo.setWorkFlowName(
				ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
		
		IOEventType ioEventType = IOEventType.ERROR_BOUNDRY;
		
		String output = "";
		IOResponse<Object> response = ioEventService.getpayload(joinPoint, ioeventRecordInfo.getBody());
		
		output = simpleEventSendProcess(ioEvent, ioFlow, response, output, ioeventRecordInfo, ioEventType, throwable);
		prepareAndDisplayEventLogger(eventLogger, ioeventRecordInfo, ioEvent, output, watch, response.getBody(), ioEventType);

		}
		//Error End Event
		else {
			if(ioEvent.endEvent().
			
			log.info("Exception caught : "+throwable.getMessage());
		}
	}
	public boolean hasTobeHandled(Class <? extends Throwable>[] listOfExceptions, Throwable throwable) {
		boolean found = false;
		int i = 0;
		while(i<listOfExceptions.length) {

			if(throwable.getClass().getCanonicalName().equals(listOfExceptions[i].getCanonicalName())) {
				found = true;
				break;
			}else {
				i++;
			}
		}
		return found;
	}

	private String simpleEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response, String output,
			IOEventRecordInfo ioeventRecordInfo, IOEventType ioEventType, Throwable throwable) {
		
		OutputEvent outputEvent = ioEvent.exception().output();
		
		Message<Object> message;
		
		Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
				response.getHeaders());

		message = this.buildTransitionTaskMessage(ioEvent, ioFlow, response, outputEvent, ioeventRecordInfo,
				ioeventRecordInfo.getStartTime(), ioEventType, headers, throwable);
		kafkaTemplate.send(message);

		output += ioEventService.getOutputKey(outputEvent) + ",";
	
		return output;		
	}
	
	public Message<Object> buildTransitionTaskMessage(
			IOEvent ioEvent, 
			IOFlow ioFlow, 
			IOResponse<Object> response,
			OutputEvent outputEvent, 
			IOEventRecordInfo ioeventRecordInfo, 
			Long startTime, 
			IOEventType ioEventType,
			Map<String, Object> headers, 
			Throwable throwable) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, outputEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), ioEventService.getInputNames(ioEvent))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), ioEventService.getOutputKey(outputEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false)
				.setHeader(IOEventHeaders.ERROR_TYPE.toString(), throwable.getCause().toString())
				.setHeader(IOEventHeaders.ERROR_MESSAGE.toString(), throwable.getMessage()).build();
	}
	
	public void prepareAndDisplayEventLogger(
			EventLogger eventLogger, 
			IOEventRecordInfo ioeventRecordInfo,
			IOEvent ioEvent, 
			String output, 
			StopWatch watch,
			Object payload, 
			IOEventType ioEventType)
			throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getOutputConsumedName(), output, ioEventType.toString(), payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
