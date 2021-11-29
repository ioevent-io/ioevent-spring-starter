package com.grizzlywave.starter.configuration.postprocessor;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.grizzlywave.starter.configuration.properties.WaveProperties;

class WaveTopicBeanPostProcessorTest {
	@InjectMocks
	WaveTopicBeanPostProcessor waveTopicBeanPostProcessor = new WaveTopicBeanPostProcessor();
	@Mock
	AdminClient client;
	@Mock
	ListTopicsResult listTopicsResult;
	@Mock
	KafkaFuture<Set<String>> future;
	@Mock
	WaveProperties waveProperties;
	@Mock
	CreateTopicsResult createResult;
	@Mock
	DeleteTopicsResult deleteResult;
	@BeforeEach
	public void init() {

		MockitoAnnotations.initMocks(this);
	}
	@Test
	void topicExist_returnTrue() throws InterruptedException, ExecutionException {
		when(client.listTopics()).thenReturn(listTopicsResult);
		when(listTopicsResult.names()).thenReturn(future);
		when(future.get()).thenReturn(new HashSet<>(Arrays.asList("test-Topic1", "Topic2","test-Topic3")));
		when(waveProperties.getPrefix()).thenReturn("test-");
		Assert.assertTrue(waveTopicBeanPostProcessor.topicExist("Topic1"));
	}
	@Test
	void topicExist_returnFalse() throws InterruptedException, ExecutionException {
		when(client.listTopics()).thenReturn(listTopicsResult);
		when(listTopicsResult.names()).thenReturn(future);
		when(future.get()).thenReturn(new HashSet<>(Arrays.asList("test-Topic1", "Topic2","test-Topic3")));
		when(waveProperties.getPrefix()).thenReturn("test-");
		Assert.assertFalse(waveTopicBeanPostProcessor.topicExist("newTopic"));
	}
}
