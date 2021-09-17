package com.grizzlywave.starter.configuration.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
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
	
	@Bean
	public AdminClient AdminClient() {
		Properties properties = new Properties() {
			{
				put("bootstrap.servers", kafkaProperties.getBootstrapServers().get(0));
				put("connections.max.idle.ms", 10000);
				put("request.timeout.ms", 5000);
			}
		};
		AdminClient client = AdminClient.create(properties);
		return client;
	}
	@Bean
	public Client KsqlClient() {
		ClientOptions options = ClientOptions.create()
		        .setHost(KsqlServer)
		        .setPort(8088);
		    return Client.create(options);
	}
	
	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> config = new HashMap<>();

		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers().get(0));
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

		return new DefaultKafkaProducerFactory<>(config);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

}
