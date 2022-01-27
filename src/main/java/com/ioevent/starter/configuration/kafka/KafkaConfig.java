package com.ioevent.starter.configuration.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

	@Autowired
	private KafkaProperties kafkaProperties;

	@Value("${spring.kafka.sasl.jaas.config:NONE}")
	private String SASL_JAAS_CONFIG;
	@Value("${spring.kafka.sasl.mechanism:NONE}")
	private String PLAIN;
	@Value("${spring.kafka.security.protocol:NONE}")
	private String SASL_SSL;
	@Value("${spring.kafka.security.status:disable}")
	private String security;
	@Value("${ioevent.group_id}")
	private String kafkaGroup_id;
	@Value("${ioevent.topic_replication:1}")
	private String topicReplication;

	/**
	 * Bean to create the kafka admin client configuration,
	 * 
	 * @return AdminClient Object,
	 **/
	@Bean
	public AdminClient AdminClient() {
		Properties properties = new Properties();

		properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers().get(0));
		properties.put("connections.max.idle.ms", 10000);
		properties.put("request.timeout.ms", 20000);
		properties.put("retry.backoff.ms", 500);

		if (security.equals("enable")) {
			properties.put("security.protocol", SASL_SSL);
			properties.put("sasl.mechanism", PLAIN);
			properties.put("sasl.jaas.config", SASL_JAAS_CONFIG);
		}

		AdminClient client = AdminClient.create(properties);
		return client;
	}

	/**
	 * Bean to define the kafka stream configuration,
	 * 
	 * @return KafkaStreamsConfiguration Object,
	 **/
	@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	public KafkaStreamsConfiguration kStreamsConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaGroup_id + "_Stream");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers().get(0));
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
		props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, topicReplication);
		props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/var/lib/kafka-streamsNEW");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		props.put(ProducerConfig.LINGER_MS_CONFIG, 10);

		if (security.equals("enable")) {
			props.put("security.protocol", SASL_SSL);
			props.put("sasl.mechanism", PLAIN);
			props.put("sasl.jaas.config", SASL_JAAS_CONFIG);
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

		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers().get(0));
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		if (security.equals("enable")) {
			config.put("security.protocol", SASL_SSL);
			config.put("sasl.mechanism", PLAIN);
			config.put("sasl.jaas.config", SASL_JAAS_CONFIG);
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

		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers().get(0));
		config.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroup_id);
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 10);

		if (security.equals("enable")) {
			config.put("security.protocol", SASL_SSL);
			config.put("sasl.mechanism", PLAIN);
			config.put("sasl.jaas.config", SASL_JAAS_CONFIG);
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
