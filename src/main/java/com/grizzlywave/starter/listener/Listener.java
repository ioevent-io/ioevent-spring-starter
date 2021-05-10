package com.grizzlywave.starter.listener;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grizzlywave.starter.configuration.postprocessor.WaveBpmnPostProcessor;
import com.grizzlywave.starter.handler.ConsumerRecordsHandler;

/** Listener under dev */


public class Listener {
	private static final Logger log = LoggerFactory.getLogger(WaveBpmnPostProcessor.class);

	private volatile boolean keepConsuming = true;
	private ConsumerRecordsHandler<String, String> recordsHandler;
	private Consumer<String, String> consumer;
	private Object bean;
	private Method method;

	

	public Listener(final Consumer<String, String> consumer,
			final ConsumerRecordsHandler<String, String> recordsHandler, Object bean, Method method) {
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
				recordsHandler.process(consumerRecords,this.bean,this.method,this.consumer);
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
	/*
	 * public static void main(String[] args) throws Exception {
	 * 
	 * if (args.length < 1) { throw new IllegalArgumentException(
	 * "This program takes one argument: the path to an environment configuration file."
	 * ); }
	 * 
	 * Properties props = new Properties(); props.setProperty("bootstrap.servers",
	 * "192.168.99.100:9092"); props.setProperty("group.id", "test");
	 * props.setProperty("enable.auto.commit", "true");
	 * props.setProperty("auto.commit.interval.ms", "1000");
	 * props.setProperty("key.deserializer",
	 * "org.apache.kafka.common.serialization.StringDeserializer");
	 * props.setProperty("value.deserializer",
	 * "org.apache.kafka.common.serialization.StringDeserializer");
	 * props.setProperty("file.path",""); final String filePath =
	 * props.getProperty("file.path"); final Consumer<String, String> consumer = new
	 * KafkaConsumer<>(props); final ConsumerRecordsHandler<String, String>
	 * recordsHandler = new FileWritingRecordsHandler(Paths.get(filePath)); final
	 * Listener consumerApplication = new Listener(consumer, recordsHandler);
	 * Runtime.getRuntime().addShutdownHook(new
	 * Thread(consumerApplication::shutdown));
	 * 
	 * consumerApplication.runConsume(props); }
	 */

}
