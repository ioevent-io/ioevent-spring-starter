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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

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
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.annotations.OutputEvent;
import com.ioevent.starter.annotations.StartEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventService;

class IOEventStartAspectTest {

	@InjectMocks
	IOEventStartAspect startAspect = new IOEventStartAspect();
	@Mock
	IOEventService ioEventService;
	@Mock
	IOEventProperties iOEventProperties;
	@Mock
	JoinPoint joinPoint;
	@Mock
	KafkaTemplate<String, Object> kafkaTemplate ;
	@BeforeEach
	public void init() {

		MockitoAnnotations.initMocks(this);
	}

	/** method to test annotations **/
	@IOEvent(key = "stepname", startEvent = @StartEvent("process name"), topic = "topic", output = @OutputEvent(key = "output", topic = "topic"))
	public boolean startAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "stepname", topic = "topic", startEvent = @StartEvent(key = "process name"), output = @OutputEvent(key = "output"))
	public boolean startAnnotationMethod2() {
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
		IOEventStartAspectTest serviceTest = Mockito.spy(this);
		Assert.assertEquals(true, startAnnotationMethod());
		Assert.assertEquals(true, startAnnotationMethod2());
		Assert.assertEquals(true, simpleTaskAnnotationMethod());

	}
 
	@Test
	void buildStartMessageTest() throws NoSuchMethodException, SecurityException {
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		when(ioEventService.getOutputTopicName(Mockito.any(IOEvent.class), Mockito.any(), Mockito.any(String.class))).thenReturn("topic");
		Method method = this.getClass().getMethod("startAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		Message messageResult = startAspect.buildStartMessage(ioEvent, null,ioEventResponse,"process", "1155", ioEvent.output()[0],
				(long) 123546);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader(IOEventHeaders.CORRELATION_ID.toString(), "1155")
				.setHeader("IOEventHeaders.STEP_NAME.toString()", "stepname").setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.START.toString())
				.setHeader(IOEventHeaders.INPUT.toString(), new ArrayList<String>(Arrays.asList("Start"))).setHeader(IOEventHeaders.OUTPUT_EVENT.toString(), "output")
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), "process name").setHeader(IOEventHeaders.START_TIME.toString(), (long) 123546).build();
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		Method method2 = this.getClass().getMethod("startAnnotationMethod2", null);
		IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class);
		Message messageResult2 = startAspect.buildStartMessage(ioEvent2,null, ioEventResponse,"process", "1155", ioEvent2.output()[0],
				(long) 123546);
		assertEquals(message.getHeaders().get("kafka_topic"), messageResult2.getHeaders().get("kafka_topic"));

	}
 
	@Test
	void prepareAndDisplayEventLoggerTest() throws JsonProcessingException, NoSuchMethodException, SecurityException, ParseException {

		when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });
		Method method = this.getClass().getMethod("startAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		UUID uuid = UUID.randomUUID();
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		eventLogger.setEndTime(eventLogger.getISODate(new Date()));

		watch.start("IOEvent annotation Start Aspect");
		startAspect.prepareAndDisplayEventLogger(eventLogger, uuid, ioEvent, "process","output", "payload", watch);

		assertThatNoException();

	}
 
	@Test
	void iOEventAnnotationAspectTest() throws Throwable {
		Method method = this.getClass().getMethod("startAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		Method method2 = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class);
	    ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
	    when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getOutputs(ioEvent)).thenReturn(Arrays.asList(ioEvent.output()));
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });

		startAspect.iOEventAnnotationAspect(joinPoint, ioEvent,null);
		startAspect.iOEventAnnotationAspect(joinPoint, ioEvent2,null);


		assertThatNoException();

	}
}
