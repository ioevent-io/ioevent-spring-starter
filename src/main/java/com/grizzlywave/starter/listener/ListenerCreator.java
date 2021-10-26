package com.grizzlywave.starter.listener;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.scheduling.annotation.Async;

import com.grizzlywave.starter.annotations.v2.IOEvent;
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

	@Autowired
	private List<Listener> listeners;

	/** create listener on a single thread for the method and the topic given 
	 * @param ioEvent */
	@Async
	public Listener createListener(Object bean, Method method, IOEvent ioEvent, String topicName, String groupId,Thread t1) throws Throwable {
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", kafkaProperties.getBootstrapServers().get(0));
		props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("security.protocol", "SASL_SSL");
		props.setProperty("sasl.mechanism", "PLAIN");
		props.setProperty("sasl.jaas.config","org.apache.kafka.common.security.plain.PlainLoginModule required " +
				"username=\"IIB2526UB7AOB4HY\" password=\"gTwgqPQeZNsIenMeuyoGmSi4yD4riLWGEQ9biO/pugvzPxuX2U8RIpwM2soyj1f6\";"
		);
		props.setProperty("group.id", groupId);
		props.setProperty("topicName", topicName);
		Consumer<String, String> consumer = new KafkaConsumer<>(props);
		Listener consumerApplication = new Listener(consumer, recordsHandler, bean, method,ioEvent,topicName);
		listeners.add(consumerApplication);

		synchronized (method) {

			method.notifyAll();
		}
		log.info("listener lunched for " + method);
		consumerApplication.runConsume(props);
		return consumerApplication;
	}
}
