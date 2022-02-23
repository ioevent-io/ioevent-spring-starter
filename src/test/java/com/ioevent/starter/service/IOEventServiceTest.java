package com.ioevent.starter.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.aspectj.lang.JoinPoint;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.ioevent.starter.annotations.EndEvent;
import com.ioevent.starter.annotations.GatewaySourceEvent;
import com.ioevent.starter.annotations.GatewayTargetEvent;
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.SourceEvent;
import com.ioevent.starter.annotations.StartEvent;
import com.ioevent.starter.annotations.TargetEvent;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventParallelEventInformation;
import com.ioevent.starter.domain.IOEventType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class IOEventServiceTest {
	@Mock
	JoinPoint joinPoint;
	

	@InjectMocks
	IOEventService ioEventService = new IOEventService();

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			source = { @SourceEvent(key = "SOURCE1"), //
					@SourceEvent(key = "SOURCE2", topic = "topic2") }, gatewaySource = @GatewaySourceEvent(parallel = true, source = {
							@SourceEvent(key = "SOURCE3", topic = "topic3") }), //
			target = { @TargetEvent(key = "TARGET1", topic = "topic4"), //
					@TargetEvent(key = "TARGET2") }, //
			gatewayTarget = @GatewayTargetEvent(exclusive = true, target = {
					@TargetEvent(key = "TARGET3", topic = "topic5") }//
			))
	public boolean tryAnnotation() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			gatewaySource = @GatewaySourceEvent(parallel = true, source = {
					@SourceEvent(key = "SOURCE3", topic = "topic3") }) // //
	)
	public boolean parallelGatewayAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			gatewaySource = @GatewaySourceEvent(exclusive = true, source = {
					@SourceEvent(key = "SOURCE3", topic = "topic3") }) // //
	)
	public boolean exclusiveGatewayAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			source = @SourceEvent(key = "source", topic = "T"), target = @TargetEvent(key = "target", topic = "T"))
	public boolean simpleTaskAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1"//
			,startEvent =  @StartEvent(key = "startkey")) // //
	public boolean startAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1",endEvent = @EndEvent(key = "endkey"))
	public boolean endAnnotationMethod() {
		return true;
	}

	@Test
	 void testTryAnnotationmethod() {
		IOEventServiceTest serviceTest = Mockito.spy(this);
		when(serviceTest.tryAnnotation()).thenReturn(true);
		when(serviceTest.parallelGatewayAnnotationMethod()).thenReturn(true);
		Assert.assertEquals(true, tryAnnotation());
		Assert.assertEquals(true, parallelGatewayAnnotationMethod());
		Assert.assertEquals(true, exclusiveGatewayAnnotationMethod());
		Assert.assertEquals(true, simpleTaskAnnotationMethod());
		Assert.assertEquals(true, startAnnotationMethod());
		Assert.assertEquals(true, endAnnotationMethod());


	}
	
	@Mock
	KafkaTemplate<String, Object> kafkaTemplate ;
	@BeforeEach
	public void init() {

		MockitoAnnotations.initMocks(this);
	}
	@Test
	void givenKeyValue_whenSend_thenVerifyHistory() {

	    ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
	    when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
	    ioEventService.sendParallelEventInfo(new IOEventParallelEventInformation("aaa", Arrays.asList("5"), new HashMap<String, Object>() {{
	        put("key","value");}}, "aazz", "fff", "fgyj", Arrays.asList("5"), new HashMap<String, Object>() {{
	        put(IOEventHeaders.CORRELATION_ID.toString(), "value1");
	    }}));
	    Assert.assertTrue(true);
	}
	@Test
	 void getSourceEventByName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		SourceEvent sourceEvent= ioEventService.getSourceEventByName(ioEvent,"SOURCE2");
		Assert.assertEquals( "topic2",sourceEvent.topic());
		Assert.assertEquals(null,ioEventService.getSourceEventByName(ioEvent, "sourceNotExist"));

		
	}
	

	@Test
	 void shouldReturnAllAnnotationSourceName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> sourceList = ioEventService.getSourceNames(ioEvent);
			List<String> sourceName = new ArrayList<String>();
			sourceName.add("SOURCE1");
			sourceName.add("SOURCE2");
			sourceName.add("SOURCE3");
			Assert.assertArrayEquals(sourceName.toArray(), sourceList.toArray());

		
	}

	@Test
	 void shouldReturnAllAnnotationParallelSourceName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> sourceList = ioEventService.getParalleListSource(ioEvent);
			List<String> sourceName = new ArrayList<String>();
			sourceName.add("SOURCE3");
			Assert.assertArrayEquals(sourceName.toArray(), sourceList.toArray());

		
	}

	@Test
	 void shouldReturnAllAnnotationTargetName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> targetList = ioEventService.getTargetNames(ioEvent);
			List<String> targetName = new ArrayList<String>();
			targetName.add("TARGET1");
			targetName.add("TARGET2");
			targetName.add("TARGET3");
			Assert.assertArrayEquals(targetName.toArray(), targetList.toArray());

		

	}

	@Test
	 void shouldReturnAllAnnotationTopicName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> topiclist = ioEventService.getTopics(ioEvent);
			List<String> topicName = new ArrayList<String>();
			topicName.add("topic1");
			topicName.add("topic2");
			topicName.add("topic3");
			topicName.add("topic4");
			topicName.add("topic5");

			Assert.assertTrue(topicName.size() == topiclist.size() && topicName.containsAll(topiclist)
					&& topiclist.containsAll(topicName));

		

	}

	@Test
	 void shouldReturnAllAnnotationSourceTopicName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> topiclist = ioEventService.getSourceTopic(ioEvent,null);
			List<String> topicName = new ArrayList<String>();
			topicName.add("topic1");
			topicName.add("topic2");
			topicName.add("topic3");

			Assert.assertTrue(topicName.size() == topiclist.size() && topicName.containsAll(topiclist)
					&& topiclist.containsAll(topicName));

		

	}

	@Test
	 void shouldReturnAllAnnotationSourceAnotations() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<SourceEvent> Sourcelist = ioEventService.getSources(ioEvent);
			Assert.assertEquals(3,Sourcelist.size());


		

	}

	@Test
	 void shouldReturnAllAnnotationTargetAnotations() throws Throwable, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<TargetEvent> targetlist = ioEventService.getTargets(ioEvent);
			Assert.assertEquals(3,targetlist.size());

		
	}

	@Test
	 void checkTaskTypeTest() throws NoSuchMethodException, SecurityException {
		Method method1 = this.getClass().getMethod("parallelGatewayAnnotationMethod", null);
		Method method2 = this.getClass().getMethod("exclusiveGatewayAnnotationMethod", null);
		Method method3 = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		Assert.assertEquals(IOEventType.GATEWAY_PARALLEL,ioEventService.checkTaskType(method1.getAnnotation(IOEvent.class)));
		Assert.assertEquals(IOEventType.GATEWAY_EXCLUSIVE,ioEventService.checkTaskType(method2.getAnnotation(IOEvent.class)));
		Assert.assertEquals(IOEventType.TASK,ioEventService.checkTaskType(method3.getAnnotation(IOEvent.class)));

	}
	@Test
	 void getIOEventTypeTest() throws NoSuchMethodException, SecurityException {
		Method method1 = this.getClass().getMethod("startAnnotationMethod", null);
		Method method2 = this.getClass().getMethod("endAnnotationMethod", null);
		Method method3 = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		Assert.assertEquals(IOEventType.START,ioEventService.getIOEventType(method1.getAnnotation(IOEvent.class))
				);
		Assert.assertEquals(IOEventType.END,ioEventService.getIOEventType(method2.getAnnotation(IOEvent.class))
				);
		Assert.assertEquals(IOEventType.TASK,ioEventService.getIOEventType(method3.getAnnotation(IOEvent.class)));

	}
	@Test
	 void sameListTest() {
		List<String> l1 = Arrays.asList("1","2","5");
		List<String> l2 = Arrays.asList("1","2","5");
		List<String> l3 = Arrays.asList("1","2");
		List<String> l4 = Arrays.asList("1","5");
		Assert.assertTrue(ioEventService.sameList(l1, l2));
		Assert.assertFalse(ioEventService.sameList(l3, l1));
		Assert.assertFalse(ioEventService.sameList(l3, l4));
		Assert.assertFalse(ioEventService.sameList(l4, l3));


	}
	@Test
	void getpayload(){
	Map<String, Object> headersMap=new HashMap<String, Object>();
	when(joinPoint.getArgs()).thenReturn(new String[] { "payload" });
	headersMap.put("firstheader", "2");
	headersMap.put("anotherHeader", 1159);
		IOResponse<String> ioEventResponse=new IOResponse<String>("our payload",headersMap);
		assertEquals(ioEventResponse, ioEventService.getpayload(null, ioEventResponse));
		assertEquals(ioEventResponse.getBody(), ioEventService.getpayload(null, "our payload").getBody());
	}
	@Test
	void prepareHeaders() {
		ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<String, String>("topic", 1, 152, 11125,
				TimestampType.LOG_APPEND_TIME, null, 0, 0, null, null, new RecordHeaders());
		consumerRecord.headers().add(IOEventHeaders.TARGET_EVENT.toString(), "target name".getBytes());
		consumerRecord.headers().add(IOEventHeaders.CORRELATION_ID.toString(), "id".getBytes());
		consumerRecord.headers().add(IOEventHeaders.PROCESS_NAME.toString(), "workflow name".getBytes());
		consumerRecord.headers().add("another header", "value".getBytes());
	List<Header> headers=Arrays.asList(consumerRecord.headers().toArray());
	Map<String, Object> headersMap=new HashMap<String, Object>();
	headersMap.put("firstheader", "2");
	headersMap.put("anotherHeader", 1159);
	assertEquals(6, ioEventService.prepareHeaders(headers, headersMap).size());
	assertEquals(2, ioEventService.prepareHeaders(null, headersMap).size());

	}

}
