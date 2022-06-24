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




package com.ioevent.starter.service;






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

import com.ioevent.starter.configuration.properties.IOEventProperties;

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
	private IOEventProperties iOEventProperties;

	@Autowired
	private AdminClient client;

	/**
	 * get a list of all topics
	 * 
	 * @return list of topics names,
	 **/
	public List<String> getAllTopic() throws InterruptedException, ExecutionException {

		ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
		listTopicsOptions.listInternal(true);
		return client.listTopics(listTopicsOptions).names().get().stream()
				.filter(a -> a.startsWith(iOEventProperties.getPrefix())).collect(Collectors.toList());
	}

	/**
	 * create new topic named topicName
	 * 
	 * @param topicName   for the topic name,
	 * @param replication for the replication value,
	 * @param prefix      for the ioevent prefix,
	 **/
	public void createTopic(String topicName, String prefix, String replication,int partition) {

		CreateTopicsResult result = client
				.createTopics(Arrays.asList(new NewTopic(prefix + topicName, partition, Short.valueOf(replication))));
		log.info(result.toString());
	}

	/**
	 * Delete the topic "topicName"
	 * 
	 * @param topicName for the topic name,
	 **/
	public void deleteTopic(String topicName) {
		client.deleteTopics(Arrays.asList(topicName));
	}
}
