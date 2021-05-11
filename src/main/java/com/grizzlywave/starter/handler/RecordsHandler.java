package com.grizzlywave.starter.handler;

import java.lang.reflect.Method;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/** Records handler to invoke method when consuming records from topic */
public class RecordsHandler {

	public RecordsHandler() {
	}

	public void process(ConsumerRecords<String, String> consumerRecords, Object bean, Method method) throws Throwable {
		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {

			this.doInvoke(method, bean, consumerRecord.value());
		}

	}

	public Object doInvoke(Method method, Object bean, Object args) throws Throwable {
		return method.invoke(bean, args);

	}
}
