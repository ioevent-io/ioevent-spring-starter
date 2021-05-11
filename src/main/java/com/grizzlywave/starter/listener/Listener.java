package com.grizzlywave.starter.listener;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import com.grizzlywave.starter.handler.RecordsHandler;

import lombok.extern.slf4j.Slf4j;

/** Listener to consume from topic and send the records consumed to the Records Handler */

@Slf4j
public class Listener {

	private volatile boolean keepConsuming = true;
	private RecordsHandler recordsHandler;
	private Consumer<String, String> consumer;
	private Object bean;
	private Method method;

	

	public Listener(final Consumer<String, String> consumer,
			final RecordsHandler recordsHandler, Object bean, Method method) {
		this.consumer = consumer;
		this.recordsHandler = recordsHandler;
		this.bean=bean;
		this.method=method;
	}
	
	public void runConsume(final Properties consumerProps) throws Throwable {
		try {
			consumer.subscribe(Collections.singletonList(consumerProps.getProperty("topicName")));
			while (keepConsuming) {
				final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(1));
				recordsHandler.process(consumerRecords,this.bean,this.method);
				   			}
		} finally {
			consumer.close();
		}
	}

	public void shutdown() {
		keepConsuming = false;
	}

	public static Properties loadProperties(String fileName) throws IOException {
		final Properties props = new Properties();
		final FileInputStream input = new FileInputStream(fileName);
		props.load(input);
		input.close();
		return props;
	}
	public Object getBean() {
		return bean;
	}

	public void setBean(Object bean) {
		this.bean = bean;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}
}
