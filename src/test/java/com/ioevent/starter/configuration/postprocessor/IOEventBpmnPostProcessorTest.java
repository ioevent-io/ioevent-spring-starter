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




package com.ioevent.starter.configuration.postprocessor;






import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Value;

import com.ioevent.starter.annotations.EndEvent;
import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.annotations.OutputEvent;
import com.ioevent.starter.annotations.StartEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventBpmnPart;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.listener.Listener;
import com.ioevent.starter.service.IOEventService;

class IOEventBpmnPostProcessorTest {
	@Value("${spring.application.name}")
	private String appName;
	@InjectMocks
	IOEventBpmnPostProcessor ioeventBpmnPostProcessor = new IOEventBpmnPostProcessor();
	@Mock
	IOEventService ioEventService;
	@Mock
	IOEventProperties iOEventProperties;
	@BeforeEach
	public void init() {

		MockitoAnnotations.initMocks(this);
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", //
			input = @InputEvent(key = "input", topic = "T"), output = @OutputEvent(key = "output", topic = "T"))
	public boolean simpleTaskAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1"//
			, startEvent = @StartEvent(key = "startkey")) // //
	public boolean startAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(key = "test annotation", topic = "topic1", endEvent = @EndEvent(key = "endkey"))
	public boolean endAnnotationMethod() {
		return true;
	}

	@Test
	void testTryAnnotationmethod() {
		IOEventBpmnPostProcessorTest serviceTest = Mockito.spy(this);
		Assert.assertEquals(true, simpleTaskAnnotationMethod());
		Assert.assertEquals(true, startAnnotationMethod());
		Assert.assertEquals(true, endAnnotationMethod());

	}

	@Test
	void Create_Start_ioEventBpmnPart() throws NoSuchMethodException, SecurityException {
		Method startMethod = this.getClass().getMethod("startAnnotationMethod", null);
		IOEvent ioEventStart = startMethod.getAnnotation(IOEvent.class);
		UUID bpmnPartId = UUID.randomUUID();
		when(ioEventService.getIOEventType(ioEventStart)).thenReturn(IOEventType.START);
		when(ioEventService.getProcessName(ioEventStart,null,"")).thenReturn("startkey");
		when(iOEventProperties.getApikey()).thenReturn("");
		when(ioEventService.getApiKey(iOEventProperties, null)).thenReturn("");
		IOEventBpmnPart ioEventBpmnPartCreated = ioeventBpmnPostProcessor.createIOEventBpmnPart(ioEventStart,null, "testClass",
				bpmnPartId.toString(), "testMethod");
		IOEventBpmnPart ioEventBpmnPart = new IOEventBpmnPart(ioEventStart, bpmnPartId.toString(),"",appName, "startkey", IOEventType.START,
				"test annotation", "testMethod");
		
		assertEquals(ioEventBpmnPart.getWorkflow(), ioEventBpmnPartCreated.getWorkflow());

	}

	@Test
	void Create_End_ioEventBpmnPart() throws NoSuchMethodException, SecurityException {
	
		Method endMethod = this.getClass().getMethod("endAnnotationMethod", null);
		IOEvent ioEventEnd = endMethod.getAnnotation(IOEvent.class);
		UUID bpmnPartId = UUID.randomUUID();
		when(ioEventService.getIOEventType(ioEventEnd)).thenReturn(IOEventType.END);
		when(ioEventService.getProcessName(ioEventEnd,null,"")).thenReturn("endkey");
		when(iOEventProperties.getApikey()).thenReturn("");
		when(ioEventService.getApiKey(iOEventProperties, null)).thenReturn("");
		IOEventBpmnPart ioEventBpmnPartCreated = ioeventBpmnPostProcessor.createIOEventBpmnPart(ioEventEnd,null, "testClass",
				bpmnPartId.toString(), "testMethod");
		IOEventBpmnPart ioEventBpmnPart = new IOEventBpmnPart(ioEventEnd, bpmnPartId.toString(),"",appName, "endkey", IOEventType.END,
				"test annotation", "testMethod");
		
		assertEquals(ioEventBpmnPart.getWorkflow(), ioEventBpmnPartCreated.getWorkflow());

	}

	@Test
	void Create_Task_ioEventBpmnPart() throws NoSuchMethodException, SecurityException {
		Method taskMethod = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEventTask = taskMethod.getAnnotation(IOEvent.class);
		UUID bpmnPartId = UUID.randomUUID();
		when(ioEventService.getIOEventType(ioEventTask)).thenReturn(IOEventType.TASK);
		when(ioEventService.getProcessName(ioEventTask,null,"")).thenReturn("");
		when(iOEventProperties.getApikey()).thenReturn("");
		when(ioEventService.getApiKey(iOEventProperties, null)).thenReturn("");
		IOEventBpmnPart ioEventBpmnPartCreated = ioeventBpmnPostProcessor.createIOEventBpmnPart(ioEventTask,null, "testClass",
				bpmnPartId.toString(), "testMethod");
		IOEventBpmnPart ioEventBpmnPart = new IOEventBpmnPart(ioEventTask, bpmnPartId.toString(),"",appName, "", IOEventType.TASK,
				"test annotation", "testMethod");
		
		assertEquals(ioEventBpmnPart.getWorkflow(), ioEventBpmnPartCreated.getWorkflow());

	}
	@Spy
	 List<Listener> listeners =new ArrayList<Listener>();
 //	new Listener(null, null, null, null, null, "Topic");
	
	@Test
	void ListenerExist_returnTrue() throws InterruptedException, NoSuchMethodException, SecurityException {
		Method taskMethod = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		when(iOEventProperties.getPrefix()).thenReturn("test_");
		listeners.add(new Listener(null, null, this, taskMethod, null, "test_Topic"));
		assertTrue(ioeventBpmnPostProcessor.listenerExist("Topic", this, taskMethod, null));
	}
	@Test
	void ListenerExist_returnFalse() throws InterruptedException {
		
		assertFalse(ioeventBpmnPostProcessor.listenerExist("Topic", null, null, null));
	}
}
