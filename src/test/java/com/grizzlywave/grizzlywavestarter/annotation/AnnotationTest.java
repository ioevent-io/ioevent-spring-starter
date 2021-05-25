package com.grizzlywave.grizzlywavestarter.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import com.grizzlywave.starter.annotations.v2.GatewaySourceEvent;
import com.grizzlywave.starter.annotations.v2.GatewayTargetEvent;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * test for our annotations call
 **/
@Slf4j
public class AnnotationTest {

	@InjectMocks
	IOEventService ioEventService = new IOEventService();

	@IOEvent(name = "test annotation", topic = "topic1", //
			source = { @SourceEvent(name = "SOURCE1"), //
					@SourceEvent(name = "SOURCE2", topic = "topic2") }, gatewaySource = @GatewaySourceEvent(parallel = true, source = {
							@SourceEvent(name = "SOURCE3", topic = "topic3") }), //
			target = { @TargetEvent(name = "TARGET1", topic = "topic4"), //
					@TargetEvent(name = "TARGET2") }, //
			gatewayTarget = @GatewayTargetEvent(exclusive = true, target = {
					@TargetEvent(name = "TARGET3", topic = "topic5") }//
			))
	public void tryAnnotation() {

	}

	@Test
	public void shouldReturnAllAnnotationSourceName() {
		for (Method method : this.getClass().getMethods()) {
			IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			if (ioEvent != null) {
				List<String> sourceList = ioEventService.getSourceNames(ioEvent);
				List<String> sourceName = new ArrayList<String>();
				sourceName.add("SOURCE1");
				sourceName.add("SOURCE2");
				sourceName.add("SOURCE3");
				Assert.assertArrayEquals(sourceName.toArray(), sourceList.toArray());

			}

		}
	}

	@Test
	public void shouldReturnAllAnnotationTargetName() {
		for (Method method : this.getClass().getMethods()) {
			IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			if (ioEvent != null) {
				List<String> targetList = ioEventService.getTargetNames(ioEvent);
				List<String> targetName = new ArrayList<String>();
				targetName.add("TARGET1");
				targetName.add("TARGET2");
				targetName.add("TARGET3");
				Assert.assertArrayEquals(targetName.toArray(), targetList.toArray());

			}

		}
	}

	@Test
	public void shouldReturnAllAnnotationTopicName() {
		for (Method method : this.getClass().getMethods()) {
			IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			if (ioEvent != null) {
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
		}

	}

	@Test
	public void shouldReturnAllAnnotationSourceTopicName() {
		for (Method method : this.getClass().getMethods()) {
			IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			if (ioEvent != null) {
				List<String> topiclist = ioEventService.getSourceTopic(ioEvent);
				List<String> topicName = new ArrayList<String>();
				topicName.add("topic1");
				topicName.add("topic2");
				topicName.add("topic3");

				Assert.assertTrue(topicName.size() == topiclist.size() && topicName.containsAll(topiclist)
						&& topiclist.containsAll(topicName));

			}

		}

	}
	@Test
	public void shouldReturnAllAnnotationSourceAnotations() {
		for (Method method : this.getClass().getMethods()) {
			IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			if (ioEvent != null) {
				List<SourceEvent> Sourcelist = ioEventService.getSources(ioEvent);
				Assert.assertTrue(Sourcelist.size()==3);

			}

		}
		
	}
	@Test
	public void shouldReturnAllAnnotationTargetAnotations() {
		for (Method method : this.getClass().getMethods()) {
			IOEvent ioEvent = method.getAnnotation(IOEvent.class);
			if (ioEvent != null) {
				List<TargetEvent> targetlist = ioEventService.getTargets(ioEvent);
				Assert.assertTrue(targetlist.size()==3);

			}

		}
		
	}
}
