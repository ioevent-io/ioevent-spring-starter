package com.grizzlywave.grizzlywavestarter.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import com.grizzlywave.grizzlywavestarter.service.TopicServices;

/**
 * configuration of Kafka Topics to (Create or Delete) topics , to create
 * default topics , to get the list of all topics
 **/
@Configuration
@ConditionalOnClass(TopicServices.class)
public class KafkaTopicConfiguration {

	/*
	 * this bean will be created if this bean dose not already exist it return the
	 * TopicService
	 */
	@Bean
	@ConditionalOnMissingBean
	public TopicServices topicService() {

		return new TopicServices();
	}

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> config = new HashMap<>();
		config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.99.100:9092");
		return new KafkaAdmin(config);
	}

	/**
	 * default topic creation
	 **/
	@Bean
	public NewTopic topicExample() {
		return TopicBuilder.name("FirstDefaultTopic").partitions(6).build();
	}
}

//@EnableKafka
//@Configuration
//public class KafkaTopicConfiguration {
//
//	@Bean
//	public KafkaAdmin kafkaAdmin() {
//		Map<String, Object> config = new HashMap<>();
//		config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.99.100:9092");
//		return new KafkaAdmin(config);
//	}
//
//	@Bean
//	public NewTopic topicExample() {
//		return TopicBuilder.name("first").partitions(6).build();
//	}}
/**
 * Properties properties = new Properties(); properties.put("bootstrap.servers",
 * "192.168.99.100:9092"); properties.put("connections.max.idle.ms", 10000);
 * properties.put("request.timeout.ms", 5000); AdminClient client =
 * AdminClient.create(properties);
 */
