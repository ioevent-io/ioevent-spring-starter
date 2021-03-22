package com.grizzlywave.grizzlywavestarter.service;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Component;

@Component
public class TopicServices {
	Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
	Properties properties = new Properties() {
		{
			put("bootstrap.servers", "192.168.99.100:9092");
			put("connections.max.idle.ms", 10000);
			put("request.timeout.ms", 5000);
		}
	};
	AdminClient client = AdminClient.create(properties);


	public void getAllTopic() throws InterruptedException, ExecutionException {

		ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
		listTopicsOptions.listInternal(true);
		LOGGER.info(client.listTopics(listTopicsOptions).names().get().toString());
	}

	public void createTopic(String topucName) {
		
		CreateTopicsResult result = client.createTopics(Arrays.asList(new NewTopic(topucName, 1, (short) 1)));
		LOGGER.info(result.toString());
	}
	public void deleteTopic(String topicName) {
		client.deleteTopics(Arrays.asList(topicName));
	} 
}
