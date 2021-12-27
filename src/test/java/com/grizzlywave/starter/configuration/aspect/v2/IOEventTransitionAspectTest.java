package com.grizzlywave.starter.configuration.aspect.v2;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import com.grizzlywave.starter.annotations.v2.GatewayTargetEvent;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.IOEventResponse;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.StartEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

class IOEventTransitionAspectTest {

	@InjectMocks
	IOEventTransitionAspect transitionAspect = new IOEventTransitionAspect();
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
	@IOEvent(name = "terminate the event", topic = "Topic", startEvent = @StartEvent(key = "process name")//
			, target = @TargetEvent(name = "target"))
	public boolean startAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "stepname", source = @SourceEvent(name = "previous Task"), //
			endEvent = @EndEvent(key = "process name"))
	public boolean endAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "GeneralTopic", //
			source = @SourceEvent(name = "source", topic = "topic"), target = @TargetEvent(name = "target", topic = "topic"))
	public boolean simpleTaskAnnotationMethod() {
		return true;
	}

	@IOEvent(name = "parallel gatway task", topic = "topic", //
			source = @SourceEvent(name = "sourceEvent"), //
			gatewayTarget = @GatewayTargetEvent(parallel = true, target = { //
					@TargetEvent(name = "Target1"), //
					@TargetEvent(name = "Target2")//
			}))
	public boolean parralelTaskAnnotationMethod() {
		return true;
	}

	@IOEvent(name = "exclusive gatway task", topic = "topic", //
			source = @SourceEvent(name = "sourceEvent"), //
			gatewayTarget = @GatewayTargetEvent(exclusive = true, target = { //
					@TargetEvent(name = "Target2"), //
					@TargetEvent(name = "Target1")//
			}))
	public boolean exclusiveTaskAnnotationMethod() {
		return true;
	}

	@IOEvent(name = "add suffix task", topic = "topic", //
			source = { @SourceEvent(name = "previous target"), //
			}, //
			target = @TargetEvent(suffix = "_suffixAdded"))
	public boolean suffixTaskAnnotation() {
		return true;
	}

	@Test
	void testTryAnnotationmethod() {
		IOEventTransitionAspectTest serviceTest = Mockito.spy(this);
		Assert.assertEquals(true, startAnnotationMethod());
		Assert.assertEquals(true, endAnnotationMethod());
		Assert.assertEquals(true, simpleTaskAnnotationMethod());
		Assert.assertEquals(true, parralelTaskAnnotationMethod());
		Assert.assertEquals(true, exclusiveTaskAnnotationMethod());
		Assert.assertEquals(true, suffixTaskAnnotation());

	}

	@Test
	void isTransitionTest() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		Method startMethod = this.getClass().getMethod("startAnnotationMethod", null);
		IOEvent startIOEvent = startMethod.getAnnotation(IOEvent.class);
		Method endMethod = this.getClass().getMethod("endAnnotationMethod", null);
		IOEvent endIOEvent = endMethod.getAnnotation(IOEvent.class);
	//	Assert.assertTrue(transitionAspect.isTransition(ioEvent));
	//	Assert.assertFalse(transitionAspect.isTransition(startIOEvent));
	//	Assert.assertFalse(transitionAspect.isTransition(endIOEvent));

	}

	@Test
	void buildTransitionTaskMessageTest() throws NoSuchMethodException, SecurityException {
		when(waveProperties.getPrefix()).thenReturn("test-");
		Method method = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "recordTarget", new StopWatch());
		Message messageResult = transitionAspect.buildTransitionTaskMessage(ioEvent,null, "payload", ioEvent.target()[0],
				waveRecordInfo, (long) 123546, IOEventType.TASK);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader("Correlation_id", "1155")
				.setHeader("StepName", "test annotation").setHeader("EventType", IOEventType.TASK.toString())
				.setHeader("source", new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader("targetEvent", "target").setHeader("Process_Name", "process name")
				.setHeader("Start Time", (long) 123546).build();

		assertEquals(message.getHeaders().get("StepName"), messageResult.getHeaders().get("StepName"));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * waveRecordInfo, (long) 123546); assertEquals("test-",
		 * messageResult2.getHeaders().get("kafka_topic"));
		 */
	}

	@Test
	void buildSuffixMessageTest() throws NoSuchMethodException, SecurityException {
		when(waveProperties.getPrefix()).thenReturn("test-");
		Method method = this.getClass().getMethod("suffixTaskAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		when(ioEventService.getSourceEventByName(Mockito.any(), Mockito.any())).thenReturn(ioEvent.source()[0]);
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "previous targe", new StopWatch());
		Message messageResult = transitionAspect.buildSuffixMessage(ioEvent,null, "payload", ioEvent.target()[0],
				waveRecordInfo, (long) 123546, IOEventType.TASK);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader("Correlation_id", "1155")
				.setHeader("StepName", "test annotation").setHeader("EventType", IOEventType.TASK.toString())
				.setHeader("source", new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader("targetEvent", waveRecordInfo.getTargetName() + "_suffixAdded")
				.setHeader("Process_Name", "process name").setHeader("Start Time", (long) 123546).build();

		assertEquals(message.getHeaders().get("targetEvent"), messageResult.getHeaders().get("targetEvent"));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * waveRecordInfo, (long) 123546); assertEquals("test-",
		 * messageResult2.getHeaders().get("kafka_topic"));
		 */
	}

	@Test
	void buildTransitionGatewayParallelMessageTest() throws NoSuchMethodException, SecurityException {
		when(waveProperties.getPrefix()).thenReturn("test-");
		Method method = this.getClass().getMethod("parralelTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "recordTarget", new StopWatch());
		Message messageResult = transitionAspect.buildTransitionGatewayParallelMessage(ioEvent,null, "payload",
				ioEvent.gatewayTarget().target()[0], waveRecordInfo, (long) 123546);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader("Correlation_id", "1155")
				.setHeader("StepName", "test annotation")
				.setHeader("EventType", IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader("source", new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader("targetEvent", "Target1").setHeader("Process_Name", "process name")
				.setHeader("Start Time", (long) 123546).build();

		assertEquals(message.getHeaders().get("targetEvent"), messageResult.getHeaders().get("targetEvent"));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * waveRecordInfo, (long) 123546); assertEquals("test-",
		 * messageResult2.getHeaders().get("kafka_topic"));
		 */
	}

	@Test
	void buildTransitionGatewayExclusiveMessage() throws NoSuchMethodException, SecurityException {
		when(waveProperties.getPrefix()).thenReturn("test-");
		Method method = this.getClass().getMethod("exclusiveTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "recordTarget", new StopWatch());
		Message messageResult = transitionAspect.buildTransitionGatewayExclusiveMessage(ioEvent,null, "payload",
				ioEvent.gatewayTarget().target()[0], waveRecordInfo, (long) 123546);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader("Correlation_id", "1155")
				.setHeader("StepName", "test annotation")
				.setHeader("EventType", IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader("source", new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader("targetEvent", "Target2").setHeader("Process_Name", "process name")
				.setHeader("Start Time", (long) 123546).build();

		assertEquals(message.getHeaders().get("targetEvent"), messageResult.getHeaders().get("targetEvent"));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * waveRecordInfo, (long) 123546); assertEquals("test-",
		 * messageResult2.getHeaders().get("kafka_topic"));
		 */
	}

	@Test
	void prepareAndDisplayEventLoggerTest() throws JsonProcessingException, NoSuchMethodException, SecurityException {

		Method method = this.getClass().getMethod("suffixTaskAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		StopWatch watch = new StopWatch();
		watch.start("IOEvent annotation Task Aspect");
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "target", watch);

		transitionAspect.prepareAndDisplayEventLogger(eventLogger, waveRecordInfo, ioEvent, "target", watch, "payload",
				IOEventType.TASK);

		assertThatNoException();

	}

	@Test
	void simpleEventSendProcessTest() throws ParseException, NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		Method method2 = this.getClass().getMethod("suffixTaskAnnotation", null);
		IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEvent)).thenReturn(Arrays.asList(ioEvent.target()));
		when(ioEventService.getTargets(ioEvent2)).thenReturn(Arrays.asList(ioEvent2.target()));
		when(ioEventService.getSourceEventByName(Mockito.any(), Mockito.any())).thenReturn(ioEvent.source()[0]);
		when(waveProperties.getPrefix()).thenReturn("test-");
		WaveRecordInfo waveRecordInfoForSuffix = new WaveRecordInfo("1155", "process name", "previous target",
				new StopWatch());
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Start Aspect");
		String simpleTasktarget = transitionAspect.simpleEventSendProcess(ioEvent,null, "payload", "", waveRecordInfo,
				eventLogger, IOEventType.TASK);
		String suffixTasktarget = transitionAspect.simpleEventSendProcess(ioEvent2,null, "payload", "",
				waveRecordInfoForSuffix, eventLogger, IOEventType.TASK);
		assertEquals("target,", simpleTasktarget);
		assertEquals("previous target_suffixAdded", suffixTasktarget);

	}

	@Test
	void parallelEventSendProcessTest() throws ParseException, NoSuchMethodException, SecurityException {

		Method method = this.getClass().getMethod("parralelTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEvent)).thenReturn(Arrays.asList(ioEvent.gatewayTarget().target()));
		when(waveProperties.getPrefix()).thenReturn("test-");
		WaveRecordInfo waveRecordInfoForSuffix = new WaveRecordInfo("1155", "process name", "previous target",
				new StopWatch());
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Start Aspect");
		String simpleTasktarget = transitionAspect.parallelEventSendProcess(ioEvent,null, "payload", "", waveRecordInfo,
				eventLogger);
		assertEquals("Target1,Target2,", simpleTasktarget);

	}

	@Test
	void exclusiveEventSendProcessTest() throws NoSuchMethodException, SecurityException, ParseException {

		Method method = this.getClass().getMethod("exclusiveTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEvent)).thenReturn(Arrays.asList(ioEvent.gatewayTarget().target()));
		WaveRecordInfo waveRecordInfoForSuffix = new WaveRecordInfo("1155", "process name", "previous target",
				new StopWatch());
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Start Aspect");
		String simpleTasktarget = transitionAspect.exclusiveEventSendProcess(ioEvent,null,
				new IOEventResponse<String>("Target2", "payload"), "", waveRecordInfo, eventLogger);
		assertEquals("Target2,", simpleTasktarget);

	}

	@Test
	void iOEventAnnotationAspectTest() throws Throwable   {
		Method methodSimpleTask = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEventSimpleTask = methodSimpleTask.getAnnotation(IOEvent.class);
		Method methodExclusive = this.getClass().getMethod("exclusiveTaskAnnotationMethod", null);
		IOEvent ioEventExclusive = methodExclusive.getAnnotation(IOEvent.class);
		Method methodParallel = this.getClass().getMethod("parralelTaskAnnotationMethod", null);
		IOEvent ioEventParallel = methodParallel.getAnnotation(IOEvent.class);
		Method endMethod = this.getClass().getMethod("endAnnotationMethod", null);
		IOEvent ioEventEnd = endMethod.getAnnotation(IOEvent.class);
		
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		StopWatch watch = new StopWatch();
		watch.start("IOEvent annotation Task Aspect");
		WaveContextHolder.setContext(waveRecordInfo);
		when(waveRecordInfo.getWatch()).thenReturn(watch);
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEventSimpleTask)).thenReturn(Arrays.asList(ioEventSimpleTask.target()));
		when(ioEventService.checkTaskType(ioEventSimpleTask)).thenReturn(IOEventType.TASK);
		when(waveProperties.getPrefix()).thenReturn("test-");
		when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });

		transitionAspect.transitionAspect(joinPoint, ioEventSimpleTask, "payload");
		transitionAspect.transitionAspect(joinPoint, ioEventExclusive, new IOEventResponse<String>("Target2", "payload"));
		transitionAspect.transitionAspect(joinPoint, ioEventParallel, "payload");
		transitionAspect.transitionAspect(joinPoint, ioEventEnd, "payload");
	


	}

}
