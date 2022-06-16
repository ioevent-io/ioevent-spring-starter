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
import com.ioevent.starter.annotations.GatewayInputEvent;
import com.ioevent.starter.annotations.GatewayOutputEvent;
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOResponse;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.annotations.StartEvent;
import com.ioevent.starter.annotations.OutputEvent;
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
			input = { @InputEvent(key = "INPUT1"), //
					@InputEvent(key = "INPUT2", topic = "topic2") }, gatewayInput = @GatewayInputEvent(parallel = true, input = {
							@InputEvent(key = "INPUT3", topic = "topic3") }), //
			output = { @OutputEvent(key = "OUTPUT1", topic = "topic4"), //
					@OutputEvent(key = "OUTPUT2") }, //
			gatewayOutput = @GatewayOutputEvent(exclusive = true, output = {
					@OutputEvent(key = "OUTPUT3", topic = "topic5") }//
			))
	public boolean tryAnnotation() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			gatewayInput = @GatewayInputEvent(parallel = true, input = {
					@InputEvent(key = "INPUT3", topic = "topic3") }) // //
	)
	public boolean parallelGatewayAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			gatewayInput = @GatewayInputEvent(exclusive = true, input = {
					@InputEvent(key = "INPUT3", topic = "topic3") }) // //
	)
	public boolean exclusiveGatewayAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			input = @InputEvent(key = "input", topic = "T"), output = @OutputEvent(key = "output", topic = "T"))
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
	 void getInputEventByName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
		InputEvent inputEvent= ioEventService.getInputEventByName(ioEvent,"INPUT2");
		Assert.assertEquals( "topic2",inputEvent.topic());
		Assert.assertEquals(null,ioEventService.getInputEventByName(ioEvent, "inputNotExist"));

		
	}
	

	@Test
	 void shouldReturnAllAnnotationInputName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> inputList = ioEventService.getInputNames(ioEvent);
			List<String> inputName = new ArrayList<String>();
			inputName.add("INPUT1");
			inputName.add("INPUT2");
			inputName.add("INPUT3");
			Assert.assertArrayEquals(inputName.toArray(), inputList.toArray());

		
	}

	@Test
	 void shouldReturnAllAnnotationParallelInputName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> inputList = ioEventService.getParalleListInput(ioEvent);
			List<String> inputName = new ArrayList<String>();
			inputName.add("INPUT3");
			Assert.assertArrayEquals(inputName.toArray(), inputList.toArray());

		
	}

	@Test
	 void shouldReturnAllAnnotationOutputName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> outputList = ioEventService.getOutputNames(ioEvent);
			List<String> outputName = new ArrayList<String>();
			outputName.add("OUTPUT1");
			outputName.add("OUTPUT2");
			outputName.add("OUTPUT3");
			Assert.assertArrayEquals(outputName.toArray(), outputList.toArray());

		

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
	 void shouldReturnAllAnnotationInputTopicName() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<String> topiclist = ioEventService.getInputTopic(ioEvent,null);
			List<String> topicName = new ArrayList<String>();
			topicName.add("topic1");
			topicName.add("topic2");
			topicName.add("topic3");

			Assert.assertTrue(topicName.size() == topiclist.size() && topicName.containsAll(topiclist)
					&& topiclist.containsAll(topicName));

		

	}

	@Test
	 void shouldReturnAllAnnotationInputAnotations() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<InputEvent> Inputlist = ioEventService.getInputs(ioEvent);
			Assert.assertEquals(3,Inputlist.size());


		

	}

	@Test
	 void shouldReturnAllAnnotationOutputAnotations() throws Throwable, SecurityException {
		Method method = this.getClass().getMethod("tryAnnotation", null);
		IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			List<OutputEvent> outputlist = ioEventService.getOutputs(ioEvent);
			Assert.assertEquals(3,outputlist.size());

		
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
		consumerRecord.headers().add(IOEventHeaders.OUTPUT_EVENT.toString(), "output name".getBytes());
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
