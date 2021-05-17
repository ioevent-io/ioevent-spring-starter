package com.grizzlywave.starter.listener;

import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.scheduling.annotation.Async;

import com.grizzlywave.starter.handler.RecordsHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * class service to create listener each listener will be created on a single
 * thread using @Async
 **/
@Slf4j
public class ListenerCreator {

	@Autowired
	private RecordsHandler recordsHandler;
	@Autowired
	private KafkaProperties kafkaProperties;

	/** create listener on a single thread for the method and the topic given */
	@Async
	public Listener createListener(Object bean, Method method, String topicName, String groupId) throws Throwable {
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", kafkaProperties.getBootstrapServers().get(0));
		props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("group.id", groupId);
		props.setProperty("topicName", topicName);
		Consumer<String, String> consumer = new KafkaConsumer<>(props);
		Listener consumerApplication = new Listener(consumer, recordsHandler, bean, method);
		consumerApplication.runConsume(props);
		return consumerApplication;
	}
}
