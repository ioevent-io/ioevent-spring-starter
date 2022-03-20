package com.ioevent.starter.configuration.aspect.v2;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import com.ioevent.starter.annotations.GatewayTargetEvent;
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.SourceEvent;
import com.ioevent.starter.annotations.StartEvent;
import com.ioevent.starter.annotations.TargetEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

class IOEventTransitionAspectTest {

	@InjectMocks
	IOEventTransitionAspect transitionAspect = new IOEventTransitionAspect();
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
	@IOEvent(key = "terminate the event", topic = "Topic", startEvent = @StartEvent(key = "process name")//
			, target = @TargetEvent(key = "target"))
	public boolean startAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "stepname", source = @SourceEvent(key = "previous Task"), //
			endEvent = @EndEvent(key = "process name"))
	public boolean endAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "GeneralTopic", //
			source = @SourceEvent(key = "source", topic = "topic"), target = @TargetEvent(key = "target", topic = "topic"))
	public boolean simpleTaskAnnotationMethod() {
		return true;
	}

	@IOEvent(key = "parallel gatway task", topic = "topic", //
			source = @SourceEvent(key = "sourceEvent"), //
			gatewayTarget = @GatewayTargetEvent(parallel = true, target = { //
					@TargetEvent(key = "Target1"), //
					@TargetEvent(key = "Target2")//
			}))
	public boolean parralelTaskAnnotationMethod() {
		return true;
	}

	@IOEvent(key  = "exclusive gatway task", topic = "topic", //
			source = @SourceEvent(key = "sourceEvent"), //
			gatewayTarget = @GatewayTargetEvent(exclusive = true, target = { //
					@TargetEvent(key = "Target2"), //
					@TargetEvent(key = "Target1")//
			}))
	public boolean exclusiveTaskAnnotationMethod() {
		return true;
	}

	@IOEvent(key = "add suffix task", topic = "topic", //
			source = { @SourceEvent(key = "previous target"), //
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
		// Assert.assertTrue(transitionAspect.isTransition(ioEvent));
		// Assert.assertFalse(transitionAspect.isTransition(startIOEvent));
		// Assert.assertFalse(transitionAspect.isTransition(endIOEvent));

	}

	@Test
	void buildTransitionTaskMessageTest() throws NoSuchMethodException, SecurityException {
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		Map<String, Object> headersMap=new HashMap<>();
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		Method method = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "recordTarget", new StopWatch(),1000L);
		Message messageResult = transitionAspect.buildTransitionTaskMessage(ioEvent, null, ioEventResponse,
				ioEvent.target()[0], ioeventRecordInfo, (long) 123546, IOEventType.TASK,headersMap);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader(IOEventHeaders.CORRELATION_ID.toString(), "1155")
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "test annotation")
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.TASK.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), "target")
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), "process name")
				.setHeader(IOEventHeaders.START_TIME.toString(), (long) 123546).build();

		assertEquals(message.getHeaders().get(IOEventHeaders.STEP_NAME.toString()),
				messageResult.getHeaders().get(IOEventHeaders.STEP_NAME.toString()));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * ioeventRecordInfo, (long) 123546); assertEquals("test-",
		 * messageResult2.getHeaders().get("kafka_topic"));
		 */
	}

	@Test
	void buildSuffixMessageTest() throws NoSuchMethodException, SecurityException {
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		Method method = this.getClass().getMethod("suffixTaskAnnotation", null);
		Map<String, Object> headersMap=new HashMap<>();
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		when(ioEventService.getSourceEventByName(Mockito.any(), Mockito.any())).thenReturn(ioEvent.source()[0]);
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "previous targe", new StopWatch(),1000L);
		Message messageResult = transitionAspect.buildSuffixMessage(ioEvent, null, ioEventResponse, ioEvent.target()[0],
				ioeventRecordInfo, (long) 123546, IOEventType.TASK,headersMap);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader(IOEventHeaders.CORRELATION_ID.toString(), "1155")
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "test annotation")
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.TASK.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), ioeventRecordInfo.getTargetName() + "_suffixAdded")
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), "process name")
				.setHeader(IOEventHeaders.START_TIME.toString(), (long) 123546).build();

		assertEquals(message.getHeaders().get(IOEventHeaders.TARGET_EVENT.toString()),
				messageResult.getHeaders().get(IOEventHeaders.TARGET_EVENT.toString()));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * ioeventRecordInfo, (long) 123546); assertEquals("test-",
		 * messageResult2.getHeaders().get("kafka_topic"));
		 */
	}

	@Test
	void buildTransitionGatewayParallelMessageTest() throws NoSuchMethodException, SecurityException {
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		when(ioEventService.getTargetKey(Mockito.any())).thenReturn("Target1");
		Method method = this.getClass().getMethod("parralelTaskAnnotationMethod", null);
		Map<String, Object> headersMap=new HashMap<>();
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "recordTarget", new StopWatch(),1000L);
		Message messageResult = transitionAspect.buildTransitionGatewayParallelMessage(ioEvent, null, ioEventResponse,
				ioEvent.gatewayTarget().target()[0], ioeventRecordInfo, (long) 123546,headersMap);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader(IOEventHeaders.CORRELATION_ID.toString(), "1155")
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "test annotation")
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), "Target1")
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), "process name")
				.setHeader(IOEventHeaders.START_TIME.toString(), (long) 123546).build();

		assertEquals(message.getHeaders().get(IOEventHeaders.TARGET_EVENT.toString()),
				messageResult.getHeaders().get(IOEventHeaders.TARGET_EVENT.toString()));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * ioeventRecordInfo, (long) 123546); assertEquals("test-",
		 * messageResult2.getHeaders().get("kafka_topic"));
		 */
	}

	@Test
	void buildTransitionGatewayExclusiveMessage() throws NoSuchMethodException, SecurityException {
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		Map<String, Object> headersMap=new HashMap<>();
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		Method method = this.getClass().getMethod("exclusiveTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "recordTarget", new StopWatch(),1000L);
		when(ioEventService.getTargetKey(ioEvent.gatewayTarget().target()[0])).thenReturn("Target2");		
		Message messageResult = transitionAspect.buildTransitionGatewayExclusiveMessage(ioEvent, null, ioEventResponse,
				ioEvent.gatewayTarget().target()[0], ioeventRecordInfo, (long) 123546,headersMap);
		Message<String> message = MessageBuilder.withPayload("payload").setHeader(KafkaHeaders.TOPIC, "test-topic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "1155").setHeader(IOEventHeaders.CORRELATION_ID.toString(), "1155")
				.setHeader(IOEventHeaders.STEP_NAME.toString(), "test annotation")
				.setHeader(IOEventHeaders.EVENT_TYPE.toString(), IOEventType.GATEWAY_PARALLEL.toString())
				.setHeader(IOEventHeaders.SOURCE.toString(), new ArrayList<String>(Arrays.asList("previous Task")))
				.setHeader(IOEventHeaders.TARGET_EVENT.toString(), "Target2")
				.setHeader(IOEventHeaders.PROCESS_NAME.toString(), "process name")
				.setHeader(IOEventHeaders.START_TIME.toString(), (long) 123546).build();
		
		assertEquals(message.getHeaders().get(IOEventHeaders.TARGET_EVENT.toString()),
				messageResult.getHeaders().get(IOEventHeaders.TARGET_EVENT.toString()));
		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));

		/*
		 * Method method2 = this.getClass().getMethod("endAnnotationMethod2", null);
		 * IOEvent ioEvent2 = method2.getAnnotation(IOEvent.class); Message
		 * messageResult2 = endAspect.buildEventMessage(ioEvent2, "payload", "END",
		 * ioeventRecordInfo, (long) 123546); assertEquals("test-",
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
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "target", watch,1000L);

		transitionAspect.prepareAndDisplayEventLogger(eventLogger, ioeventRecordInfo, ioEvent, "target", watch, "payload",
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
		when(ioEventService.getTargetKey(ioEvent.target()[0])).thenReturn("target");		
		when(ioEventService.getSourceEventByName(Mockito.any(), Mockito.any())).thenReturn(ioEvent.source()[0]);
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		IOEventRecordInfo ioeventRecordInfoForSuffix = new IOEventRecordInfo("1155", "process name", "previous target",
				new StopWatch(),1000L);
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Start Aspect");
		Map<String, Object> headersMap=new HashMap<>();
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		String simpleTasktarget = transitionAspect.simpleEventSendProcess(ioEvent, null, ioEventResponse, "", ioeventRecordInfo,
				IOEventType.TASK);
		String suffixTasktarget = transitionAspect.simpleEventSendProcess(ioEvent2, null, ioEventResponse, "",
				ioeventRecordInfoForSuffix, IOEventType.TASK);
		assertEquals("target,", simpleTasktarget);
		assertEquals("previous target_suffixAdded", suffixTasktarget);

	}

	@Test
	void parallelEventSendProcessTest() throws ParseException, NoSuchMethodException, SecurityException {
		Map<String, Object> headersMap=new HashMap<>();
		IOResponse<Object> ioEventResponse = new IOResponse<>(null, "payload", null);
		Method method = this.getClass().getMethod("parralelTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEvent)).thenReturn(Arrays.asList(ioEvent.gatewayTarget().target()));
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		when(ioEventService.getTargetKey(ioEvent.gatewayTarget().target()[0])).thenReturn("Target1");
		when(ioEventService.getTargetKey(ioEvent.gatewayTarget().target()[1])).thenReturn("Target2");
		IOEventRecordInfo ioeventRecordInfoForSuffix = new IOEventRecordInfo("1155", "process name", "previous target",
				new StopWatch(),1000L);
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Start Aspect");
		String simpleTasktarget = transitionAspect.parallelEventSendProcess(ioEvent, null, ioEventResponse, "",
				ioeventRecordInfo);
		assertEquals("Target1,Target2,", simpleTasktarget);

	}

	@Test
	void exclusiveEventSendProcessTest() throws NoSuchMethodException, SecurityException, ParseException {

		Method method = this.getClass().getMethod("exclusiveTaskAnnotationMethod", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEvent)).thenReturn(Arrays.asList(ioEvent.gatewayTarget().target()));
		when(ioEventService.getTargetKey(ioEvent.gatewayTarget().target()[0])).thenReturn("Target1");		
		when(ioEventService.getTargetKey(ioEvent.gatewayTarget().target()[1])).thenReturn("Target2");
		IOEventRecordInfo ioeventRecordInfoForSuffix = new IOEventRecordInfo("1155", "process name", "previous target",
				new StopWatch(),1000L);
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("IOEvent annotation Start Aspect");
		String simpleTasktarget = transitionAspect.exclusiveEventSendProcess(ioEvent, null,
				new IOResponse<String>("Target2", "payload"), "", ioeventRecordInfo);
		assertEquals("Target2,", simpleTasktarget);

	}

	@Test
	void iOEventAnnotationAspectTest() throws Throwable {
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
		IOEventContextHolder.setContext(ioeventRecordInfo);
		when(ioeventRecordInfo.getWatch()).thenReturn(watch);
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getTargets(ioEventSimpleTask)).thenReturn(Arrays.asList(ioEventSimpleTask.target()));
		when(ioEventService.checkTaskType(ioEventSimpleTask)).thenReturn(IOEventType.TASK);
		when(iOEventProperties.getPrefix()).thenReturn("test-");
		when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });

		transitionAspect.transitionAspect(joinPoint, ioEventSimpleTask, "payload");
		transitionAspect.transitionAspect(joinPoint, ioEventExclusive,
				new IOResponse<String>("Target2", "payload"));
		transitionAspect.transitionAspect(joinPoint, ioEventParallel, "payload");
		transitionAspect.transitionAspect(joinPoint, ioEventEnd, "payload");

	}

}
