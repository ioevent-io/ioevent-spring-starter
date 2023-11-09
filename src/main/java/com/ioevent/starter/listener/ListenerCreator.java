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
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
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

	@Value("${spring.kafka.bootstrap-servers}")
	private String kafkaBootstrapServer;
	@Value("${spring.kafka.sasl.jaas.username:}")
	private String saslJaasUsername;
	@Value("${spring.kafka.sasl.jaas.password:}")
	private String saslJaasPassword;
	@Value("${spring.kafka.sasl.mechanism:PLAIN}")
	private String plain;
	@Value("${spring.kafka.security.protocol:SASL_SSL}")
	private String saslSsl;
	@Autowired
	private KafkaProperties kafkaProperties;
	@Value("${spring.kafka.sasl.mechanism:NONE}")
	private String PLAIN;
	@Value("${spring.kafka.security.protocol:}")
	private String SASL_SSL;
	@Value("${ioevent.auto.offset.reset:earliest}")
	private String autoOffsetReset;
	/**
	 * create listener on a single thread for the method and the topic given
	 * 
	 * @param ioEvent
	 */

	public Listener createListener(Object bean, Method method, IOEvent ioEvent, String topicName, String groupId,
			Thread t1) throws Throwable {
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", kafkaBootstrapServer);
		props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.setProperty("group.id", groupId);
		props.setProperty("topicName", topicName);
		props.setProperty("auto.offset.reset", autoOffsetReset);
		if (!StringUtils.isBlank(saslJaasUsername)) {
			String saslJaasConfig = String.format(
					"org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
					saslJaasUsername, saslJaasPassword);
			props.put("security.protocol", saslSsl);
			props.put("sasl.mechanism", plain);
			props.put("sasl.jaas.config", saslJaasConfig);
		}

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
