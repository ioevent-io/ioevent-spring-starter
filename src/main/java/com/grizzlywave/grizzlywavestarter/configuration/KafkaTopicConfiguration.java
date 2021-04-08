package com.grizzlywave.grizzlywavestarter.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.grizzlywavestarter.service.TopicServices;

/**
 * configuration of Kafka Topics to (Create or Delete) topics , to create
 * default topics , to get the list of all topics
 **/

@Configuration
@ConditionalOnClass(TopicServices.class)
public class KafkaTopicConfiguration {
	/*
	 * @Autowired WaveConfigProperties waveProperties;
	 * 
	 * @Autowired TopicServices topicService;
	 */
	/**
	 * this bean will be created if this bean dose not already exist it return the
	 * TopicService
	 **/
	@Bean(name = "topicService")
	@ConditionalOnMissingBean
	public TopicServices topicService() {

		return new TopicServices();
	}

	/**
	 * default topic creation
	 **/

	/*
	 * @Bean
	 * 
	 * @DependsOn({"topicService"}) public void createTopic() {
	 * this.topicService().createTopic("DefaultTopic"); }
	 */

	/*
	 * @Bean public KafkaAdmin kafkaAdmin() { Map<String, Object> config = new
	 * HashMap<>(); config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
	 * "192.168.99.100:9092"); return new KafkaAdmin(config); } /* @Bean public
	 * NewTopic topicExample() { return
	 * TopicBuilder.name("Wave-DefaultTopic").partitions(6).build(); }
	 */
	/*
	 * @Bean public String topic() {
	 * waveProperties.getTopic_names().stream().forEach(x ->
	 * topicService.createTopic(x,waveProperties.getPrefix())); return
	 * waveProperties.getTopic_names().toString(); }
	 */
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
