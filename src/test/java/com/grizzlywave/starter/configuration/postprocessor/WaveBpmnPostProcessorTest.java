package com.grizzlywave.starter.configuration.postprocessor;

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

import com.grizzlywave.starter.annotations.v2.EndEvent;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.StartEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventBpmnPart;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.listener.Listener;
import com.grizzlywave.starter.service.IOEventService;

class WaveBpmnPostProcessorTest {
	@InjectMocks
	WaveBpmnPostProcessor waveBpmnPostProcessor = new WaveBpmnPostProcessor();
	@Mock
	IOEventService ioEventService;
	@Mock
	WaveProperties waveProperties;
	@BeforeEach
	public void init() {

		MockitoAnnotations.initMocks(this);
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1", //
			source = @SourceEvent(name = "source", topic = "T"), target = @TargetEvent(name = "target", topic = "T"))
	public boolean simpleTaskAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1"//
			, startEvent = @StartEvent(key = "startkey")) // //
	public boolean startAnnotationMethod() {
		return true;
	}

	/** method to test annotations **/
	@IOEvent(name = "test annotation", topic = "topic1", endEvent = @EndEvent(key = "endkey"))
	public boolean endAnnotationMethod() {
		return true;
	}

	@Test
	void testTryAnnotationmethod() {
		WaveBpmnPostProcessorTest serviceTest = Mockito.spy(this);
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
		when(waveProperties.getApikey()).thenReturn("");
		when(ioEventService.getApiKey(waveProperties, null)).thenReturn("");
		IOEventBpmnPart ioEventBpmnPartCreated = waveBpmnPostProcessor.createIOEventBpmnPart(ioEventStart,null, "testClass",
				bpmnPartId.toString(), "testMethod");
		IOEventBpmnPart ioEventBpmnPart = new IOEventBpmnPart(ioEventStart, bpmnPartId.toString(),"", "startkey", IOEventType.START,
				"test annotation", "testClass", "testMethod");
		
		assertEquals(ioEventBpmnPart.getWorkflow(), ioEventBpmnPartCreated.getWorkflow());

	}

	@Test
	void Create_End_ioEventBpmnPart() throws NoSuchMethodException, SecurityException {
	
		Method endMethod = this.getClass().getMethod("endAnnotationMethod", null);
		IOEvent ioEventEnd = endMethod.getAnnotation(IOEvent.class);
		UUID bpmnPartId = UUID.randomUUID();
		when(ioEventService.getIOEventType(ioEventEnd)).thenReturn(IOEventType.END);
		when(ioEventService.getProcessName(ioEventEnd,null,"")).thenReturn("endkey");
		when(waveProperties.getApikey()).thenReturn("");
		when(ioEventService.getApiKey(waveProperties, null)).thenReturn("");
		IOEventBpmnPart ioEventBpmnPartCreated = waveBpmnPostProcessor.createIOEventBpmnPart(ioEventEnd,null, "testClass",
				bpmnPartId.toString(), "testMethod");
		IOEventBpmnPart ioEventBpmnPart = new IOEventBpmnPart(ioEventEnd, bpmnPartId.toString(),"", "endkey", IOEventType.END,
				"test annotation", "testClass", "testMethod");
		
		assertEquals(ioEventBpmnPart.getWorkflow(), ioEventBpmnPartCreated.getWorkflow());

	}

	@Test
	void Create_Task_ioEventBpmnPart() throws NoSuchMethodException, SecurityException {
		Method taskMethod = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		IOEvent ioEventTask = taskMethod.getAnnotation(IOEvent.class);
		UUID bpmnPartId = UUID.randomUUID();
		when(ioEventService.getIOEventType(ioEventTask)).thenReturn(IOEventType.TASK);
		when(ioEventService.getProcessName(ioEventTask,null,"")).thenReturn("");
		when(waveProperties.getApikey()).thenReturn("");
		when(ioEventService.getApiKey(waveProperties, null)).thenReturn("");
		IOEventBpmnPart ioEventBpmnPartCreated = waveBpmnPostProcessor.createIOEventBpmnPart(ioEventTask,null, "testClass",
				bpmnPartId.toString(), "testMethod");
		IOEventBpmnPart ioEventBpmnPart = new IOEventBpmnPart(ioEventTask, bpmnPartId.toString(),"", "", IOEventType.TASK,
				"test annotation", "testClass", "testMethod");
		
		assertEquals(ioEventBpmnPart.getWorkflow(), ioEventBpmnPartCreated.getWorkflow());

	}
	@Spy
	 List<Listener> listeners =new ArrayList<Listener>();
 //	new Listener(null, null, null, null, null, "Topic");
	
	@Test
	void ListenerExist_returnTrue() throws InterruptedException, NoSuchMethodException, SecurityException {
		Method taskMethod = this.getClass().getMethod("simpleTaskAnnotationMethod", null);
		when(waveProperties.getPrefix()).thenReturn("test_");
		listeners.add(new Listener(null, null, this, taskMethod, null, "test_Topic"));
		assertTrue(waveBpmnPostProcessor.listenerExist("Topic", this, taskMethod, null));
	}
	@Test
	void ListenerExist_returnFalse() throws InterruptedException {
		
		assertFalse(waveBpmnPostProcessor.listenerExist("Topic", null, null, null));
	}
}
