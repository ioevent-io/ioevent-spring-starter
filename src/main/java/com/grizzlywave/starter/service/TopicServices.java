package com.grizzlywave.starter.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.grizzlywave.starter.configuration.properties.WaveProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Class TopicServices where we define services on topics (create , delete ,
 * getAllTopics...)
 **/
@Slf4j
@Primary
@Service
public class TopicServices {
	@Autowired
	private WaveProperties waveProperties;

	@Autowired
	private AdminClient client;

	/**
	 * get a list of all topics
	 **/
	public List<String> getAllTopic() throws InterruptedException, ExecutionException {

		ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
		listTopicsOptions.listInternal(true);
		return client.listTopics(listTopicsOptions).names().get().stream()
				.filter(a -> a.startsWith(waveProperties.getPrefix())).collect(Collectors.toList());
	}

	/**
	 * create new topic named topicName
	 * 
	 * @param prefix
	 **/
	public void createTopic(String topicName, String prefix) {

		CreateTopicsResult result = client.createTopics(Arrays.asList(new NewTopic(prefix + topicName, 1, (short) 1)));
		log.info(result.toString());
	}

	/**
	 * Delete the topic "topicName"
	 **/
	public void deleteTopic(String topicName) {
		client.deleteTopics(Arrays.asList(topicName));
	}
}
