package com.grizzlywave.starter.configuration.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaBootstrapConfiguration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;

/**
 * class that contain the Configuration of the Broker Kafka
 **/
public class KafkaConfig {

	@Autowired
	private KafkaProperties kafkaProperties;
	
	@Value("${Ksql.server}")
	private String KsqlServer;
	@Value("${Ksql.port}")
	private int KsqlPort;
	@Value("${spring.kafka.sasl.jaas.config:NONE}")
	private String SASL_JAAS_CONFIG;
	@Value("${spring.kafka.sasl.mechanism:NONE}")
	private String PLAIN;
	@Value("${spring.kafka.security.protocol:NONE}")
	private String SASL_SSL;
	@Value("${spring.kafka.security.status:disable}")
	private String security;

	@Bean
	public AdminClient AdminClient() throws InterruptedException {
		Properties properties = new Properties() ;

		properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers().get(0));
		properties.put("connections.max.idle.ms", 10000);
		properties.put("request.timeout.ms", 20000);
		properties.put("retry.backoff.ms", 500);

		if (security.equals("enable")){
			properties.put("security.protocol", SASL_SSL);
			properties.put("sasl.mechanism", PLAIN);
			properties.put("sasl.jaas.config",SASL_JAAS_CONFIG
			);
		}

		AdminClient client = AdminClient.create(properties);
		return client;
	}
	@Bean
	public Client KsqlClient() {
		ClientOptions options = ClientOptions.create()
		        .setHost(KsqlServer)
		        .setPort(KsqlPort);
		    return Client.create(options);
	}
	
	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> config = new HashMap<>();

		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers().get(0));
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		if (security.equals("enable")){
			config.put("security.protocol", SASL_SSL);
			config.put("sasl.mechanism", PLAIN);
			config.put("sasl.jaas.config",SASL_JAAS_CONFIG
			);
		}
		return new DefaultKafkaProducerFactory<>(config);
	}


	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

}
