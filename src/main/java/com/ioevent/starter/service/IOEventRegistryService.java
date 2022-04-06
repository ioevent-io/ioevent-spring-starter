package com.ioevent.starter.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;

import com.ioevent.starter.domain.IOEventBpmnPart;
import com.ioevent.starter.domain.RegistryAction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOEventRegistryService {
	@Autowired
	private TopicServices topicServices;

	@Autowired
	private List<IOEventBpmnPart> iobpmnlist;

	@Autowired
	private Set<String> apiKeys;

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;
	@Autowired
	private UUID instanceId;

	@PreDestroy
	public void shutdownHook() throws InterruptedException, ExecutionException {
		Message<List<IOEventBpmnPart>> message = MessageBuilder.withPayload(iobpmnlist)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-apps").setHeader(KafkaHeaders.MESSAGE_KEY, appName)
				.setHeader("IO-APP-NAME", appName).setHeader("APIKEYS", apiKeys)
				.setHeader("INSTANCE-ID", instanceId.toString()).setHeader("TOPICS", topicServices.getAllTopic())
				.setHeader("ACTION", RegistryAction.CLOSE.toString()).build();
		kafkaTemplate.send(message);
	}

	@Scheduled(fixedRate = 5000)
	public void registryHeartBeat() throws InterruptedException, ExecutionException {
		Message<List<IOEventBpmnPart>> message = MessageBuilder.withPayload(iobpmnlist)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-apps").setHeader(KafkaHeaders.MESSAGE_KEY, appName)
				.setHeader("IO-APP-NAME", appName).setHeader("INSTANCE-ID", instanceId.toString())
				.setHeader("APIKEYS", apiKeys).setHeader("TOPICS", topicServices.getAllTopic())
				.setHeader("ACTION", RegistryAction.REGISTER.toString()).build();
		kafkaTemplate.send(message);

	}
}
