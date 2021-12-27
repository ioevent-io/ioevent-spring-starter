package com.grizzlywave.starter.configuration.aspect.v2;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

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
import com.grizzlywave.starter.annotations.v2.EndEvent;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

class IOEventEndAspectTest {
	@InjectMocks
	IOEventEndAspect endAspect = new IOEventEndAspect();
	@Mock
	IOEventService ioEventService;
	@Mock
	WaveProperties waveProperties;
	@Mock
	JoinPoint joinPoint;
	@Mock
	KafkaTemplate<String, Object> kafkaTemplate;
	@Mock
	WaveRecordInfo waveRecordInfo;

	@BeforeEach
	public void init() {

		MockitoAnnotations.initMocks(this);
	}

	/** method to test annotations **/
	@IOEvent(name = "terminate the event", topic = "Topic", //
			source = @SourceEvent(name = "previous Task"), //
			endEvent = @EndEvent(key = "process name")//
	)
	public boolean endAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "stepname",source = @SourceEvent(name = "previous Task"), //
			endEvent = @EndEvent(key = "process name"))
	public boolean endAnnotationMethod2() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1", //
			source = @SourceEvent(name = "source", topic = "T"), target = @TargetEvent(name = "target", topic = "T"))
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
		when(waveProperties.getPrefix()).thenReturn("test-");
		when(ioEventService.getTargetTopicName(Mockito.any(IOEvent.class), Mockito.any(), Mockito.any(String.class))).thenReturn("");
		Method method = this.getClass().getMethod("endAnnotationMethod", null);
		
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "_", new StopWatch());
		Message messageResult = endAspect.buildEventMessage(ioEvent,null, "payload", "END", waveRecordInfo, (long) 123546);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader("Correlation_id", "1155")
				.setHeader("StepName", "terminate the event").setHeader("EventType", IOEventType.END.toString())
				.setHeader("source", new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader("targetEvent", "END").setHeader("Process_Name", "process name")
				.setHeader("Start Time", (long) 123546).build();

		assertEquals(message.getHeaders().get("StepName"), messageResult.getHeaders().get("StepName"));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class);
		Message messageResult2 = endAspect.buildEventMessage(ioEvent2,null, "payload", "END", waveRecordInfo, (long) 123546);
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
				waveRecordInfo);
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
		WaveContextHolder.setContext(waveRecordInfo);
		when(waveRecordInfo.getWatch()).thenReturn(watch);
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEvent)).thenReturn(Arrays.asList(ioEvent.target()));
		when(waveProperties.getPrefix()).thenReturn("test-");
		when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });

		endAspect.iOEventAnnotationAspect(joinPoint, ioEvent, "payload");
		endAspect.iOEventAnnotationAspect(joinPoint, ioEvent2, "payload");
		assertThatNoException();


	}

}
