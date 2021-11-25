package com.grizzlywave.starter.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.tomcat.util.digester.ArrayStack;
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

import com.grizzlywave.starter.annotations.v2.EndEvent;
import com.grizzlywave.starter.annotations.v2.GatewaySourceEvent;
import com.grizzlywave.starter.annotations.v2.GatewayTargetEvent;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.StartEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class IOEventServiceTest {

	@InjectMocks
	IOEventService ioEventService = new IOEventService();

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1", //
			source = { @SourceEvent(name = "SOURCE1"), //
					@SourceEvent(name = "SOURCE2", topic = "topic2") }, gatewaySource = @GatewaySourceEvent(parallel = true, source = {
							@SourceEvent(name = "SOURCE3", topic = "topic3") }), //
			target = { @TargetEvent(name = "TARGET1", topic = "topic4"), //
					@TargetEvent(name = "TARGET2") }, //
			gatewayTarget = @GatewayTargetEvent(exclusive = true, target = {
					@TargetEvent(name = "TARGET3", topic = "topic5") }//
			))
	public boolean tryAnnotation() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1", //
			gatewaySource = @GatewaySourceEvent(parallel = true, source = {
					@SourceEvent(name = "SOURCE3", topic = "topic3") }) // //
	)
	public boolean parallelGatewayAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1", //
			gatewaySource = @GatewaySourceEvent(exclusive = true, source = {
					@SourceEvent(name = "SOURCE3", topic = "topic3") }) // //
	)
	public boolean exclusiveGatewayAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1", //
			source = @SourceEvent(name = "source", topic = "T"), target = @TargetEvent(name = "target", topic = "T"))
	public boolean simpleTaskAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1"//
			,startEvent =  @StartEvent(key = "startkey")) // //
	public boolean startAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1",endEvent = @EndEvent(key = "endkey"))
	public boolean endAnnotationMethod() {
		return true;
	}

	@Test
	public void testTryAnnotationmethod() {
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
		//KafkaTemplate kafkaTemplate = Mockito.mock(KafkaTemplate.class);

	    ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
	    when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
	    ioEventService.sendParallelEventInfo(new WaveParallelEventInformation("aaa", Arrays.asList("5"), "aazz", "fff", "fgyj", Arrays.asList("5"), new HashMap<String, String>() {{
	        put("Correlation_id", "value1");
	    }}));
	}
	@Test
	public void getSourceEventByName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		SourceEvent sourceEvent= ioEventService.getSourceEventByName(ioEvent,"SOURCE2");
		Assert.assertEquals(sourceEvent.topic(), "topic2");
		Assert.assertEquals(ioEventService.getSourceEventByName(ioEvent, "sourceNotExist"), null);

		
	}
	

	@Test
	public void shouldReturnAllAnnotationSourceName() throws NoSuchMethodException, SecurityException {
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
	public void shouldReturnAllAnnotationParallelSourceName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> sourceList = ioEventService.getParalleListSource(ioEvent);
			List<String> sourceName = new ArrayList<String>();
			sourceName.add("SOURCE3");
			Assert.assertArrayEquals(sourceName.toArray(), sourceList.toArray());

		
	}

	@Test
	public void shouldReturnAllAnnotationTargetName() throws NoSuchMethodException, SecurityException {
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
	public void shouldReturnAllAnnotationTopicName() throws NoSuchMethodException, SecurityException {
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
	public void shouldReturnAllAnnotationSourceTopicName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> topiclist = ioEventService.getSourceTopic(ioEvent);
			List<String> topicName = new ArrayList<String>();
			topicName.add("topic1");
			topicName.add("topic2");
			topicName.add("topic3");

			Assert.assertTrue(topicName.size() == topiclist.size() && topicName.containsAll(topiclist)
					&& topiclist.containsAll(topicName));

		

	}

	@Test
	public void shouldReturnAllAnnotationSourceAnotations() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<SourceEvent> Sourcelist = ioEventService.getSources(ioEvent);
			Assert.assertTrue(Sourcelist.size() == 3);


		

	}

	@Test
	public void shouldReturnAllAnnotationTargetAnotations() throws Throwable, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<TargetEvent> targetlist = ioEventService.getTargets(ioEvent);
			Assert.assertTrue(targetlist.size() == 3);

		
	}

	@Test
	public void checkTaskTypeTest() throws NoSuchMethodException, SecurityException {
		Method method1 = this.getClass().getMethod("parallelGatewayAnnotationMethod", null);
		Method method2 = this.getClass().getMethod("exclusiveGatewayAnnotationMethod", null);
		Method method3 = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		Assert.assertTrue(ioEventService.checkTaskType(method1.getAnnotation(IOEvent.class))
				.equals(IOEventType.GATEWAY_PARALLEL));
		Assert.assertTrue(ioEventService.checkTaskType(method2.getAnnotation(IOEvent.class))
				.equals(IOEventType.GATEWAY_EXCLUSIVE));
		Assert.assertTrue(ioEventService.checkTaskType(method3.getAnnotation(IOEvent.class)).equals(IOEventType.TASK));

	}
	@Test
	public void getIOEventTypeTest() throws NoSuchMethodException, SecurityException {
		Method method1 = this.getClass().getMethod("startAnnotationMethod", null);
		Method method2 = this.getClass().getMethod("endAnnotationMethod", null);
		Method method3 = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		Assert.assertTrue(ioEventService.getIOEventType(method1.getAnnotation(IOEvent.class))
				.equals(IOEventType.START));
		Assert.assertTrue(ioEventService.getIOEventType(method2.getAnnotation(IOEvent.class))
				.equals(IOEventType.END));
		Assert.assertTrue(ioEventService.getIOEventType(method3.getAnnotation(IOEvent.class)).equals(IOEventType.TASK));

	}
	@Test
	public void sameListTest() {
		List<String> l1 = Arrays.asList("1","2","5");
		List<String> l2 = Arrays.asList("1","2","5");
		List<String> l3 = Arrays.asList("1","2");
		List<String> l4 = Arrays.asList("1","5");
		Assert.assertEquals(ioEventService.sameList(l1, l2), true);
		Assert.assertEquals(ioEventService.sameList(l3, l1), false);
		Assert.assertEquals(ioEventService.sameList(l3, l4), false);
		Assert.assertEquals(ioEventService.sameList(l4, l3), false);


	}
}
