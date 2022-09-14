package com.ioevent.starter.configuration.aspect.v2;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
	 * Method AfterThrowing advice is executed after a join point does not complete
	 * normally and end up throwing an exception.
	 * @param joinPoint for the join point during the execution of the program,
	 * @param ioEvent   for ioevent annotation which include task information,
	 * @param throwable for throwed exception during the execution,
	 * @throws ParseException
	 * @throws JsonProcessingException
	 */
	@AfterThrowing(value = "@annotation(anno)", argNames = "jp, anno,ex", throwing = "ex")
	public void throwingExceptionAspect(JoinPoint joinPoint, IOEvent ioEvent, Throwable throwable)
			throws ParseException, JsonProcessingException {
		IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		StopWatch watch = ioeventRecordInfo.getWatch();
		IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
		ioeventRecordInfo.setWorkFlowName(ioEventService.getProcessName(ioEvent, ioFlow, ioeventRecordInfo.getWorkFlowName()));
		
		if (ioEvent.exception().exception().length != 0 && hasTobeHandled(ioEvent.exception().exception(), throwable)) {			
			IOEventType ioEventType = IOEventType.ERROR_BOUNDRY;
			eventLogger.setErrorType(throwable.getClass().getCanonicalName());
			String output = "";
			IOResponse<Object> response = ioEventService.getpayload(joinPoint, ioeventRecordInfo.getBody());
			
			if (!StringUtils.isBlank(ioEvent.exception().endEvent().value())) {
				ioEventType = IOEventType.ERROR_END;
				output = endEventSendProcess(ioEvent, ioFlow, response, output, ioeventRecordInfo, ioEventType, throwable);
			} else {
				output = simpleEventSendProcess(ioEvent, ioFlow, response, output, ioeventRecordInfo, ioEventType, throwable);
			}
			prepareAndDisplayEventLogger(eventLogger, ioeventRecordInfo, ioEvent, output, watch, response.getBody(),
					ioEventType);
		}
		else {
				IOEventType ioEventType = IOEventType.UNHANDLED_ERROR;
				eventLogger.setErrorType(throwable.getClass().getCanonicalName());
				IOResponse<Object> response = ioEventService.getpayload(joinPoint, ioeventRecordInfo.getBody());
				String output ="";
				output = simpleEventSendProcess(ioEvent, ioFlow, response, output, ioeventRecordInfo, ioEventType, throwable);
				prepareAndDisplayEventLogger(eventLogger, ioeventRecordInfo, ioEvent, output, watch, response.getBody(), ioEventType);
		}
	}

	private String endEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload, String output,
			IOEventRecordInfo ioeventRecordInfo, IOEventType ioEventType, Throwable throwable) {
		output = "ERROR END";
		Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
				payload.getHeaders());
		Message<Object> message = this.buildEndEventMessage(ioEvent, ioFlow, payload, output, ioeventRecordInfo,
				ioeventRecordInfo.getStartTime(), headers, throwable);
		kafkaTemplate.send(message);
		return null;
	}

	public boolean hasTobeHandled(Class<? extends Throwable>[] listOfExceptions, Throwable throwable) {
		boolean found = false;
		int i = 0;
		while (i < listOfExceptions.length) {

			if (throwable.getClass().getCanonicalName().equals(listOfExceptions[i].getCanonicalName())) {
				found = true;
				break;
			} else {
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
		if(ioEventType.equals(IOEventType.UNHANDLED_ERROR)) {
			output += "IOEvent_Unhandled_Exception";
		}else {
			output += ioEventService.getOutputKey(outputEvent) + ",";
		}
		return output;
	}
	public Message<Object> buildEndEventMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> payload,
			String outputEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime, Map<String, Object> headers, Throwable throwable) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, "");
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		return MessageBuilder.withPayload(payload.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), outputEvent)
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.ERROR_END.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), ioEventService.getInputNames(ioEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime()).setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false)
				.setHeader(IOEventHeaders.ERROR_TYPE.toString(), throwable.getClass().getCanonicalName())
				.setHeader(IOEventHeaders.ERROR_MESSAGE.toString(), throwable.getMessage())
				.setHeader(IOEventHeaders.ERROR_TRACE.toString(), Arrays.toString(ExceptionUtils.getRootCauseStackTrace(throwable)))
				.build();
	}
	public Message<Object> buildTransitionTaskMessage(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response,
			OutputEvent outputEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime, IOEventType ioEventType,
			Map<String, Object> headers, Throwable throwable) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, outputEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		String nextOutput = ioEventService.getOutputKey(outputEvent);
		if(ioEventType.equals(IOEventType.UNHANDLED_ERROR)) {
			nextOutput = "IOEvent_Unhandled_Exception";
		}
		
		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), ioEventService.getInputNames(ioEvent))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), nextOutput)
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false)
				.setHeader(IOEventHeaders.ERROR_TYPE.toString(), throwable.getClass().getCanonicalName())
				.setHeader(IOEventHeaders.ERROR_MESSAGE.toString(), throwable.getMessage())
				.setHeader(IOEventHeaders.ERROR_TRACE.toString(), Arrays.toString(ExceptionUtils.getRootCauseStackTrace(throwable)))

				.build();
	}

	public void prepareAndDisplayEventLogger(EventLogger eventLogger, IOEventRecordInfo ioeventRecordInfo,
			IOEvent ioEvent, String output, StopWatch watch, Object payload, IOEventType ioEventType)
			throws JsonProcessingException {
		watch.stop();
		eventLogger.loggerSetting(ioeventRecordInfo.getId(), ioeventRecordInfo.getWorkFlowName(), ioEvent.key(),
				ioeventRecordInfo.getOutputConsumedName(), output, ioEventType.toString(), payload);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}
}
