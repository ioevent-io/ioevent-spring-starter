package com.ioevent.starter.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;

import com.ioevent.starter.domain.IOEventBpmnPart;
import com.ioevent.starter.domain.RegistryAction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOEventRegistryService implements ApplicationListener<WebServerInitializedEvent> {
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
	private int port;

	@PreDestroy
	public void shutdownHook() throws InterruptedException, ExecutionException, UnknownHostException {
		Message<List<IOEventBpmnPart>> message = MessageBuilder.withPayload(iobpmnlist)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-apps").setHeader(KafkaHeaders.MESSAGE_KEY, appName)
				.setHeader("IO-APP-NAME", appName).setHeader("APIKEYS", apiKeys)
				.setHeader("INSTANCE-ID", instanceId.toString())
				.setHeader("IO-APP-HOST", InetAddress.getLocalHost().getHostAddress()).setHeader("IO-APP-PORT", port)
				.setHeader("TOPICS", topicServices.getAllTopic()).setHeader("ACTION", RegistryAction.CLOSE.toString())
				.build();
		kafkaTemplate.send(message);
	}

	@Scheduled(fixedRate = 6000)
	public void registryHeartBeat() throws InterruptedException, ExecutionException, UnknownHostException {
		Message<List<IOEventBpmnPart>> message = MessageBuilder.withPayload(iobpmnlist)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-apps").setHeader(KafkaHeaders.MESSAGE_KEY, appName)
				.setHeader("IO-APP-NAME", appName).setHeader("INSTANCE-ID", instanceId.toString())
				.setHeader("IO-APP-HOST", InetAddress.getLocalHost().getHostAddress()).setHeader("IO-APP-PORT", port)
				.setHeader("APIKEYS", apiKeys).setHeader("TOPICS", topicServices.getAllTopic())
				.setHeader("ACTION", RegistryAction.REGISTER.toString()).build();
		kafkaTemplate.send(message);

	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		this.port = event.getWebServer().getPort();
	}
}
