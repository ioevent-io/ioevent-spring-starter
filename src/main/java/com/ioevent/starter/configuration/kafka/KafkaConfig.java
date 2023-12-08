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




package com.ioevent.starter.configuration.kafka;






import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.kafka.support.serializer.JsonSerializer;

import lombok.extern.slf4j.Slf4j;

/**
 * class that contain the Configuration of the Broker Kafka
 **/
@Slf4j
public class KafkaConfig {


	@Value("${spring.kafka.group-id:default-admin-group}")
	private String kafkaGroupId;
	@Value("${spring.kafka.sasl.jaas.username:}")
	private String saslJaasUsername;
	@Value("${spring.kafka.sasl.jaas.password:}")
	private String saslJaasPassword;
	@Value("${spring.kafka.sasl.mechanism:PLAIN}")
	private String plain;
	@Value("${spring.kafka.security.protocol:SASL_SSL}")
	private String saslSsl;
	@Value("${spring.kafka.bootstrap-servers}")
	private String kafkaBootstrapServer;
	@Value("${spring.kafka.ssl-truststore-location:}")
	private String sslTruststoreLocation;
	@Value("${spring.kafka.ssl-truststore-password:}")
	private String sslTruststorePassword;
	@Value("${spring.kafka.ssl-keystore-location:}")
	private String sslKeystoreLocation;
	@Value("${spring.kafka.ssl-keystore-password:}")
	private String sslKeystorePassword;
	@Value("${spring.kafka.properties.ssl.endpoint.identification.algorithm:}")
	private String sslEndpointIdentificationAlgorithm;
	//@Autowired
	//private KafkaProperties kafkaProperties;
	
	@Value("${spring.kafka.sasl.mechanism:NONE}")
	private String PLAIN;
	@Value("${spring.kafka.security.protocol:}")
	private String SASL_SSL;
	
	@Value("${spring.kafka.state.dir:/tmp/var/lib/kafka-streams-newconfluent8}")
	private String stateDir;
	
	@Value("#{'${spring.kafka.consumer.group-id:${ioevent.group_id:${spring.application.name:ioevent_default_groupid}}}'}")
	private String kafkaGroup_id;
	@Value("${spring.kafka.streams.replication-factor:1}")
	private String topicReplication;

    private static final String SECURITY_PROTOCOl = "security.protocol";
	private static final String SSL_TRUSTSTORE_LOCATION = "ssl.truststore.location";
	private static final String SSL_TRUSTSTORE_PASSWORD = "ssl.truststore.password";
	private static final String SSL_KEYSTORE_LOCATION = "ssl.keystore.location";
	private static final String SSL_KEYSTORE_PASSWORD = "ssl.keystore.password";
	private static final String SSL_ENDPOINT_IDENTIFICATION_ALGORITHM = "ssl.endpoint.identification.algorithm";
    private static final String SASL_MECHANISM = "sasl.mechanism";
	private static final String SASL_JAAS_CONFIG = "sasl.jaas.config";
	/**
	 * Bean to create the kafka admin client configuration,
	 * 
	 * @return AdminClient Object,
	 **/
	@Bean
	public AdminClient adminClient() {
		Properties properties = new Properties();

		properties.put("bootstrap.servers", kafkaBootstrapServer);
		properties.put("connections.max.idle.ms", 10000);
		properties.put("request.timeout.ms", 20000);
		properties.put("retry.backoff.ms", 500);

		if (!StringUtils.isBlank(saslJaasUsername)) {
			String saslJaasConfig = String.format(
					"org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
					saslJaasUsername, saslJaasPassword);
			properties.put(SECURITY_PROTOCOl, saslSsl);
			properties.put(SASL_MECHANISM, plain);
			properties.put(SASL_JAAS_CONFIG, saslJaasConfig);
		}
		if(!StringUtils.isBlank(sslTruststoreLocation)) {
			properties.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM, sslEndpointIdentificationAlgorithm);
			properties.put(SSL_TRUSTSTORE_LOCATION, sslTruststoreLocation);
			properties.put(SSL_TRUSTSTORE_PASSWORD, sslTruststorePassword);
		}
		if(!StringUtils.isBlank(sslKeystoreLocation)) {
			properties.put(SSL_KEYSTORE_LOCATION, sslKeystoreLocation);
			properties.put(SSL_KEYSTORE_PASSWORD, sslKeystorePassword);
		}
		return AdminClient.create(properties);
	}

	/**
	 * Bean to define the kafka stream configuration,
	 * 
	 * @return KafkaStreamsConfiguration Object,
	 **/
	@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	public KafkaStreamsConfiguration kStreamsConfigs() {
		Map<String, Object> props = new HashMap<>();
		kafkaGroup_id = kafkaGroup_id.replaceAll("\\s+","");
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaGroup_id + "_Stream");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServer);
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
		props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, topicReplication);
		props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		props.put(ProducerConfig.LINGER_MS_CONFIG, 5);

		if (!StringUtils.isBlank(saslJaasUsername)) {
			String saslJaasConfig = String.format(
					"org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
					saslJaasUsername, saslJaasPassword);
			props.put(SECURITY_PROTOCOl, saslSsl);
			props.put(SASL_MECHANISM, plain);
			props.put(SASL_JAAS_CONFIG, saslJaasConfig);
		}
		if(!StringUtils.isBlank(sslTruststoreLocation)) {
			props.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM, sslEndpointIdentificationAlgorithm);
			props.put(SSL_TRUSTSTORE_LOCATION, sslTruststoreLocation);
			props.put(SSL_TRUSTSTORE_PASSWORD, sslTruststorePassword);
		}
		if(!StringUtils.isBlank(sslKeystoreLocation)) {
			props.put(SSL_KEYSTORE_LOCATION, sslKeystoreLocation);
			props.put(SSL_KEYSTORE_PASSWORD, sslKeystorePassword);
		}
		return new KafkaStreamsConfiguration(props);
	}

	/**
	 * Bean to define the kafka producer configuration,
	 * 
	 * @return ProducerFactory Object,
	 **/
	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> config = new HashMap<>();

		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServer);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		
		if (!StringUtils.isBlank(saslJaasUsername)) {
			String saslJaasConfig = String.format(
					"org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
					saslJaasUsername, saslJaasPassword);
			config.put(SECURITY_PROTOCOl, saslSsl);
			config.put(SASL_MECHANISM, plain);
			config.put(SASL_JAAS_CONFIG, saslJaasConfig);
		}
		if(!StringUtils.isBlank(sslTruststoreLocation)) {
			config.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM, sslEndpointIdentificationAlgorithm);
			config.put(SSL_TRUSTSTORE_LOCATION, sslTruststoreLocation);
			config.put(SSL_TRUSTSTORE_PASSWORD, sslTruststorePassword);
		}
		if(!StringUtils.isBlank(sslKeystoreLocation)) {
			config.put(SSL_KEYSTORE_LOCATION, sslKeystoreLocation);
			config.put(SSL_KEYSTORE_PASSWORD, sslKeystorePassword);
		}
		return new DefaultKafkaProducerFactory<>(config);
	}

	/**
	 * Bean to init the kafka Template,
	 * 
	 * @return ProducerFactory Object,
	 **/
	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	/**
	 * Bean to define the kafka Consumer configuration,
	 * 
	 * @return ConsumerFactory Object,
	 **/
	@Bean
	public ConsumerFactory<String, String> userConsumerFactory() {
		Map<String, Object> config = new HashMap<>();

		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServer);
		config.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroup_id);
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 10);

		if (!StringUtils.isBlank(saslJaasUsername)) {
			String saslJaasConfig = String.format(
					"org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
					saslJaasUsername, saslJaasPassword);
			config.put(SECURITY_PROTOCOl, saslSsl);
			config.put(SASL_MECHANISM, plain);
			config.put(SASL_JAAS_CONFIG, saslJaasConfig);
		}
		if(!StringUtils.isBlank(sslTruststoreLocation)) {
			config.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM, sslEndpointIdentificationAlgorithm);
			config.put(SSL_TRUSTSTORE_LOCATION, sslTruststoreLocation);
			config.put(SSL_TRUSTSTORE_PASSWORD, sslTruststorePassword);
		}
		if(!StringUtils.isBlank(sslKeystoreLocation)) {
			config.put(SSL_KEYSTORE_LOCATION, sslKeystoreLocation);
			config.put(SSL_KEYSTORE_PASSWORD, sslKeystorePassword);
		}
		
		return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new StringDeserializer());
	}

	@Bean
	public DefaultKafkaHeaderMapper headerMapper() {
		return new DefaultKafkaHeaderMapper();
	}

	/**
	 * Bean to define the KafkaListenerContainerFactory,
	 * 
	 * @return ConcurrentKafkaListenerContainerFactory Object,
	 **/
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> userKafkaListenerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(userConsumerFactory());
		return factory;
	}
}
