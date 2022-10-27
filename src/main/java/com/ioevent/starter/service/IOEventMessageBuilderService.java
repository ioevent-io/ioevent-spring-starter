package com.ioevent.starter.service;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.OutputEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.handler.IOEventRecordInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IOEventMessageBuilderService {
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private IOEventProperties iOEventProperties;
	@Autowired
	private IOEventService ioEventService;

	private static final String START_PREFIX = "start-to-";

	/**
	 * Method that build and send the event of a Parallel Event task,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param response
	 * @param outputs           for the list of outputs of the event separated by
	 *                          ",",
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @return string format list of outputs of the event separated by "," ,
	 */
	public String parallelEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, IOResponse<Object> response, String outputs,
			IOEventRecordInfo ioeventRecordInfo, boolean isImplicitStart) {
		Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
				response.getHeaders());
		for (OutputEvent outputEvent : ioEventService.getOutputs(ioEvent)) {
			Message<Object> message = this.buildTransitionGatewayParallelMessage(ioEvent, ioFlow, response, outputEvent,
					ioeventRecordInfo, ioeventRecordInfo.getStartTime(), headers, isImplicitStart);
			kafkaTemplate.send(message);

			outputs += ioEventService.getOutputKey(outputEvent) + ",";
		}
		return outputs;
	}

	/**
	 * Method that build the event message of Parallel task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param response          for the response which include the payload of the
	 *                          event
	 * @param outputEvent       for the output Event where the event will send
	 * @param ioeventRecordInfo
	 * @param startTime         for the start time of the event,
	 * @param headers           for message headers
	 * @param isImplicitStart
	 * @return message type of Message,
	 */
	public Message<Object> buildTransitionGatewayParallelMessage(IOEvent ioEvent, IOFlow ioFlow,
			IOResponse<Object> response, OutputEvent outputEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime,
			Map<String, Object> headers, boolean isImplicitStart) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, outputEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader(IOEventHeaders.INPUT.toString(),
						Arrays.asList(
								isImplicitStart ? START_PREFIX + ioEvent.key() : ioEventService.getInputNames(ioEvent)))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), ioEventService.getOutputKey(outputEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), isImplicitStart)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
	}

	/**
	 * Method that build and send the event of a Exclusive Event task,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param returnObject      for the returned object,
	 * @param outputs           for the list of outputs of the event separated by
	 *                          ",",
	 * @param ioeventRecordInfo for the record information from the consumed event,
	 * @return string format list of outputs of the event separated by "," ,
	 */
	public String exclusiveEventSendProcess(IOEvent ioEvent, IOFlow ioFlow, Object returnObject, String outputs,
			IOEventRecordInfo ioeventRecordInfo, boolean isImplicitStart) {
		IOResponse<Object> ioEventResponse = IOResponse.class.cast(returnObject);
		if (ioEventService.validExclusiveOutput(ioEvent, ioEventResponse)) {
			Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
					ioEventResponse.getHeaders());
			for (OutputEvent outputEvent : ioEventService.getOutputs(ioEvent)) {
				if (ioEventResponse.getKey().equals(ioEventService.getOutputKey(outputEvent))) {
					Message<Object> message = this.buildTransitionGatewayExclusiveMessage(ioEvent, ioFlow,
							ioEventResponse, outputEvent, ioeventRecordInfo, ioeventRecordInfo.getStartTime(), headers,
							isImplicitStart);
					kafkaTemplate.send(message);

					outputs += ioEventService.getOutputKey(outputEvent) + ",";
					log.info("sent to : {}", ioEventService.getOutputKey(outputEvent));
				}

			}
			return outputs;
		}
		throw new IllegalStateException(
				"exclusive target output does not exist within the gateway outputs declared list !");

	}

	/**
	 * Method that build the event message of Exclusive task to be send in kafka
	 * topic,
	 * 
	 * @param ioEvent           for ioevent annotation which include task
	 *                          information,
	 * @param ioFlow            for ioflow annotation which include general
	 *                          information,
	 * @param response          for the response which include the payload of the
	 *                          event
	 * @param outputEvent       for the output Event where the event will send
	 * @param ioeventRecordInfo
	 * @param startTime         for the start time of the event,
	 * @param headers           for message headers
	 * @param isImplicitStart
	 * @return message type of Message,
	 */
	public Message<Object> buildTransitionGatewayExclusiveMessage(IOEvent ioEvent, IOFlow ioFlow,
			IOResponse<Object> response, OutputEvent outputEvent, IOEventRecordInfo ioeventRecordInfo, Long startTime,
			Map<String, Object> headers, boolean isImplicitStart) {
		String topicName = ioEventService.getOutputTopicName(ioEvent, ioFlow, outputEvent.topic());
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		if (response.getBody() == null) {
			response.setBody(KafkaNull.INSTANCE);
		}

		return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers)
				.setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix() + topicName)
				.setHeader(KafkaHeaders.MESSAGE_KEY, ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
				.setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.GATEWAY_EXCLUSIVE.toString())
				.setHeader(IOEventHeaders.INPUT.toString(),
						Arrays.asList(
								isImplicitStart ? START_PREFIX + ioEvent.key() : ioEventService.getInputNames(ioEvent)))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), ioEventService.getOutputKey(outputEvent))
				.setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
				.setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
				.setHeader(IOEventHeaders.START_TIME.toString(), startTime)
				.setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
				.setHeader(IOEventHeaders.IMPLICIT_START.toString(), isImplicitStart)
				.setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
	}
}
