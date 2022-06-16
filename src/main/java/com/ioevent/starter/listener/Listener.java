/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ioevent.starter.listener;




import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
import com.ioevent.starter.handler.RecordsHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener to consume from topic and send the records consumed to the Records
 * Handler
 */

@Slf4j
public class Listener {

	private volatile boolean keepConsuming = true;
	final private RecordsHandler recordsHandler;
	private Consumer<String, String> consumer;
	private Object bean;
	private Method method;
	private String topic;
	private List<BeanMethodPair> beanMethodPairs = new ArrayList<BeanMethodPair>();

	/**
	 * listener constructor
	 * 
	 * @param ioEvent
	 * @param topicName
	 */
	public Listener(final Consumer<String, String> consumer, final RecordsHandler recordsHandler, Object bean,
			Method method, IOEvent ioEvent, String topicName) {
		this.consumer = consumer;
		this.recordsHandler = recordsHandler;
		this.bean = bean;
		this.method = method;
		this.topic = topicName;
		this.beanMethodPairs.add(new BeanMethodPair(bean, method, ioEvent));

	}

	/**
	 * run consumer to subscribe to the output topic and start consuming ,as soon as
	 * we get a record we send the record to the handler
	 **/
	public void runConsume(final Properties consumerProps) throws Throwable {
		try {
			consumer.subscribe(Collections.singletonList(consumerProps.getProperty("topicName")));
			while (keepConsuming) {
				ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(10));
				if (!consumerRecords.isEmpty()) {
					recordsHandler.process(consumerRecords, this.beanMethodPairs);
				}

			}
		} finally {
			consumer.close();
		}
	}

	public void shutdown() {
		keepConsuming = false;
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

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public List<BeanMethodPair> getBeanMethodPairs() {
		return beanMethodPairs;
	}

	public void setBeanMethodPairs(List<BeanMethodPair> beanMethodPairs) {
		this.beanMethodPairs = beanMethodPairs;
	}

	public void addBeanMethod(BeanMethodPair beanMethod) {
		boolean valid = true;
		for (BeanMethodPair beanMethodPair : beanMethodPairs) {
			if ((beanMethod.getBean().equals(beanMethodPair.getBean())
					&& beanMethod.getMethod().equals(beanMethodPair.getMethod()))) {
				valid = false;
			}
		}
		if (valid) {
			this.beanMethodPairs.add(beanMethod);

		}

	}
}
