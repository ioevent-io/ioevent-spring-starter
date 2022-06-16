/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




package com.ioevent.starter.configuration.aspect.v2;






import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StopWatch;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ioevent.starter.annotations.EndEvent;
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.annotations.OutputEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

class IOEventEndAspectTest {
	@InjectMocks
	IOEventEndAspect endAspect = new IOEventEndAspect();
	@Mock
	IOEventService ioEventService;
	@Mock
	IOEventProperties iOEventProperties;
	@Mock
	JoinPoint joinPoint;
	@Mock
	KafkaTemplate<String, Object> kafkaTemplate;
	@Mock
	IOEventRecordInfo ioeventRecordInfo;

	@BeforeEach
	public void init() {

		MockitoAnnotations.initMocks(this);
	}

	/** method to test annotations **/
	@IOEvent(key = "terminate the event", topic = "Topic", //
			input = @InputEvent(key = "previous Task"), //
			endEvent = @EndEvent(key = "process name")//
	)
	public boolean endAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "stepname",input = @InputEvent(key = "previous Task"), //
			endEvent = @EndEvent(key = "process name"))
	public boolean endAnnotationMethod2() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			input = @InputEvent(key = "input", topic = "T"), output = @OutputEvent(key = "output", topic = "T"))
	public boolean simpleTaskAnnotationMethod() {
		return true;
	}

	@Test
	void testTryAnnotationmethod() {
		IOEventEndAspectTest serviceTest = Mockito.spy(this);
		Assert.assertEquals(true, endAnnotationMethod());
		Assert.assertEquals(true, endAnnotationMethod2());
		Assert.assertEquals(true, simpleTaskAnnotationMethod());

	}

	@Test
	void buildEventMessageTest() throws NoSuchMethodException, SecurityException {
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		when(ioEventService.getOutputTopicName(Mockito.any(IOEvent.class), Mockito.any(), Mockito.any(String.class))).thenReturn("");
		Method method = this.getClass().getMethod("endAnnotationMethod", null);
		
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		Map<String, Object> headersMap=new HashMap<>();
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "_", new StopWatch(),100L);
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		Message messageResult = endAspect.buildEventMessage(ioEvent,null, ioEventResponse, "END", ioeventRecordInfo, (long) 123546,headersMap);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader(IOEventHeaders.CORRELATION_ID.toString(), "1155")
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "terminate the event").setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.END.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), "END").setHeader(IOEventHeaders.PROCESS_NAME.toString(), "process name")
				.setHeader(IOEventHeaders.START_TIME.toString(), (long) 123546).build();

		assertEquals(message.getHeaders().get(IOEventHeaders.STEP_NAME.toString()), messageResult.getHeaders().get(IOEventHeaders.STEP_NAME.toString()));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class);
		Message messageResult2 = endAspect.buildEventMessage(ioEvent2,null, ioEventResponse, "END", ioeventRecordInfo, (long) 123546,headersMap);
		assertEquals("test-", messageResult2.getHeaders().get("kafka_topic"));

	}

	@Test
	void prepareAndDisplayEventLoggerTest() throws JsonProcessingException, NoSuchMethodException, SecurityException {

		when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });
		Method method = this.getClass().getMethod("endAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation End Aspect");
		endAspect.prepareAndDisplayEventLogger(eventLogger, ioEvent, "payload" , watch,
				ioeventRecordInfo);
		assertThatNoException();

	}

	@Test
	void iOEventAnnotationAspectTest() throws Throwable {
		Method method = this.getClass().getMethod("endAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		Method method2 = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		StopWatch watch = new StopWatch();
		watch.start("IOEvent annotation End Aspect");
		IOEventContextHolder.setContext(ioeventRecordInfo);
		when(ioeventRecordInfo.getWatch()).thenReturn(watch);
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getOutputs(ioEvent)).thenReturn(Arrays.asList(ioEvent.output()));
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });

		endAspect.iOEventAnnotationAspect(joinPoint, ioEvent, "payload");
		endAspect.iOEventAnnotationAspect(joinPoint, ioEvent2, "payload");
		assertThatNoException();


	}

}
