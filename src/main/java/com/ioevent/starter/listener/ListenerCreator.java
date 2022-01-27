package com.ioevent.starter.listener;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.handler.RecordsHandler;

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
	@Value("${spring.kafka.sasl.jaas.config:NONE}")
	private String SASL_JAAS_CONFIG;
	@Value("${spring.kafka.sasl.mechanism:NONE}")
	private String PLAIN;
	@Value("${spring.kafka.security.protocol:NONE}")
	private String SASL_SSL;
	@Value("${spring.kafka.security.status:disable}")
	private String security;
	/** create listener on a single thread for the method and the topic given 
	 * @param ioEvent */

	public Listener createListener(Object bean, Method method, IOEvent ioEvent, String topicName, String groupId,Thread t1) throws Throwable {
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", kafkaProperties.getBootstrapServers().get(0));
		props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("group.id", groupId);
		props.setProperty("topicName", topicName);

		if (security.equals("enable")){
			props.put("security.protocol", SASL_SSL);
			props.put("sasl.mechanism", PLAIN);
			props.put("sasl.jaas.config",SASL_JAAS_CONFIG
			);
		}
		Consumer<String, String> consumer = new KafkaConsumer<>(props);
		Listener consumerApplication = new Listener(consumer, recordsHandler, bean, method,ioEvent,topicName);
		listeners.add(consumerApplication);

		synchronized (method) {

			method.notify();
		}
		log.info("listener lunched for " + method);
		consumerApplication.runConsume(props);
		return consumerApplication;
	}
}
