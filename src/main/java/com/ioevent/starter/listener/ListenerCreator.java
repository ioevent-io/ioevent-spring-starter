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
import java.util.List;
import java.util.Map;

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
	private List<Listener> listeners;

	@Autowired
	private KafkaProperties kafkaProperties;
	@Value("${ioevent.auto.offset.reset:earliest}")
	private String autoOffsetReset;
	/**
	 * create listener on a single thread for the method and the topic given
	 * 
	 * @param ioEvent
	 */

	public Listener createListener(Object bean, Method method, IOEvent ioEvent, String topicName, String groupId,
			Thread t1) throws Throwable {
		Map<String,Object> props = kafkaProperties.buildConsumerProperties();
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("group.id", groupId);
		props.put("topicName", topicName);
		props.put("auto.offset.reset", autoOffsetReset);


		Consumer<String, String> consumer = new KafkaConsumer<>(props);
		Listener consumerApplication = new Listener(consumer, recordsHandler, bean, method, ioEvent, topicName);
		listeners.add(consumerApplication);

		synchronized (method) {

			method.notify();
		}
		log.info("listener lunched for " + method);
		consumerApplication.runConsume(props);
		return consumerApplication;
	}
}
