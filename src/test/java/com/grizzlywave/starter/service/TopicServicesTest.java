package com.grizzlywave.starter.service;

import static org.hamcrest.CoreMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;

class TopicServicesTest {
	@InjectMocks
	TopicServices topicServices = new TopicServices();
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
	void getAllTopicTest() throws InterruptedException, ExecutionException {
		when(client.listTopics(Mockito.any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
		when(listTopicsResult.names()).thenReturn(future);
		when(future.get()).thenReturn(new HashSet<>(Arrays.asList("test-Topic1", "Topic2")));
		when(waveProperties.getPrefix()).thenReturn("test");
		Assert.assertEquals(topicServices.getAllTopic(), Arrays.asList("test-Topic1"));
	}
	@Test
	void createTopicTest()   {
		when(client.createTopics(Mockito.anyCollectionOf(NewTopic.class))).thenReturn(createResult);
		topicServices.createTopic("Topic", "test-", "3");
	}
	@Test
	void deleteTopicTest()   {
		when(client.deleteTopics(Mockito.anyCollection())).thenReturn(deleteResult);
		topicServices.deleteTopic("test-Topic");
	}
}
