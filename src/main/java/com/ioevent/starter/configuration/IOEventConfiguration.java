package com.ioevent.starter.configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.ioevent.starter.configuration.aspect.v2.IOEvenImplicitTaskAspect;
import com.ioevent.starter.configuration.aspect.v2.IOEventEndAspect;
import com.ioevent.starter.configuration.aspect.v2.IOEventStartAspect;
import com.ioevent.starter.configuration.aspect.v2.IOEventTransitionAspect;
import com.ioevent.starter.configuration.kafka.KafkaConfig;
import com.ioevent.starter.configuration.postprocessor.IOEventBpmnPostProcessor;
import com.ioevent.starter.configuration.postprocessor.IOEventTopicBeanPostProcessor;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.controller.IOEventController;
import com.ioevent.starter.domain.IOEventBpmnPart;
import com.ioevent.starter.domain.IOEventParallelEventInformation;
import com.ioevent.starter.handler.RecordsHandler;
import com.ioevent.starter.listener.IOEventParrallelListener;
import com.ioevent.starter.listener.Listener;
import com.ioevent.starter.listener.ListenerCreator;
import com.ioevent.starter.service.IOEventRegistryService;
import com.ioevent.starter.service.IOEventService;
import com.ioevent.starter.service.TopicServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * class for ioevent configuration which contains all configurations needed by a
 * project which use IOEvent
 **/
@Slf4j
@EnableKafka
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableKafkaStreams
@EnableScheduling
@EnableAsync
@Import({ KafkaConfig.class})
@Service
@RequiredArgsConstructor
public class IOEventConfiguration {

	ObjectMapper mapper = new ObjectMapper();

	@Value("${spring.application.name}")
	private String appName;

	/**
	 * method for processing parallel events from the ParallelEventTopic using kafka stream,
	 * 
	 * @param builder type of StreamsBuilder,
	 */
	@Autowired
	public void processKStream(final StreamsBuilder builder) {

		Gson gson = new Gson();

		KStream<String, String> kstream = builder
				.stream("ParallelEventTopic", Consumed.with(Serdes.String(), Serdes.String()))
				.map((k, v) -> new KeyValue<>(k, v)).filter((k, v) -> {
					IOEventParallelEventInformation value = gson.fromJson(v, IOEventParallelEventInformation.class);
					return appName.equals(value.getHeaders().get("AppName"));
				});
		kstream.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
				.aggregate(() -> "", (key, value, aggregateValue) -> {
					IOEventParallelEventInformation currentValue = gson.fromJson(value,
							IOEventParallelEventInformation.class);
					IOEventParallelEventInformation updatedValue;
					if (!aggregateValue.isBlank()) {
						updatedValue = gson.fromJson(aggregateValue, IOEventParallelEventInformation.class);
					} else {
						updatedValue = currentValue;
					}
					List<String> updatedOutputList = Stream
							.of(currentValue.getInputsArrived(), updatedValue.getInputsArrived())
							.flatMap(x -> x.stream()).distinct().collect(Collectors.toList());
					Map<String, Object> updatedHeaders = Stream.of(currentValue.getHeaders(), updatedValue.getHeaders())
							.flatMap(map -> map.entrySet().stream())
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
					Map<String, Object> updatedPayload = Stream.of(currentValue.getPayloadMap(), updatedValue.getPayloadMap())
							.flatMap(map -> map.entrySet().stream())
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
					updatedValue.setInputsArrived(updatedOutputList);
					updatedValue.setHeaders(updatedHeaders);
					updatedValue.setPayloadMap(updatedPayload);
					aggregateValue = gson.toJson(updatedValue);
					return aggregateValue;
				}).toStream().to("resultTopic", Produced.with(Serdes.String(), Serdes.String()));

	}

	@Bean
	public IOEventParrallelListener IOEventParrallelListener() {
		return new IOEventParrallelListener();
	}

	@Bean
	public com.ioevent.starter.configuration.context.AppContext AppContext() {
		return new com.ioevent.starter.configuration.context.AppContext();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEventProperties IOEventProperties() {
		return new IOEventProperties();
	}

	@Bean
	public TopicServices TopicServices() {
		return new TopicServices();
	}

	@Bean
	public RecordsHandler recordsHandler() {
		return new RecordsHandler();
	}

	@Bean
	public ListenerCreator ListenerCreator() {
		return new ListenerCreator();
	}

	@Bean
	public Executor asyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("Asynchronous Process-");
		executor.initialize();
		return executor;
	}

	@Bean
	public IOEventTopicBeanPostProcessor IOEventTopicBeanPostProcessor() {
		return new IOEventTopicBeanPostProcessor();
	}

	@Bean
	public IOEventBpmnPostProcessor IOEventBpmnPostProcessor() {
		return new IOEventBpmnPostProcessor();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEventStartAspect IOEventStartAspect() {
		return new IOEventStartAspect();
	}

	@Bean
	public IOEventTransitionAspect IOEventTransitionAspect() {
		return new IOEventTransitionAspect();
	}

	@Bean
	public IOEventEndAspect IOEventEndAspect() {
		return new IOEventEndAspect();
	}

	@Bean
	public IOEvenImplicitTaskAspect IOEvenImplicitTaskAspect() {
		return new IOEvenImplicitTaskAspect();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEventController IOEventController() {
		return new IOEventController();
	}

	

	@Bean("iobpmnlist")
	public List<IOEventBpmnPart> iobpmnlist() {
		return new LinkedList<>();
	}
	
	@Bean("apiKeys")
	public Set<String> apiKeys() {
		return new HashSet<>();
	}
	@Bean("listeners")
	public List<Listener> listeners() {
		return new ArrayList<>();
	}

	@Bean
	public IOEventService IOEventService() {
		return new IOEventService();
	}
	@Bean
	public IOEventRegistryService ioeventRegistryService() {
		return new IOEventRegistryService();
	}
	@Bean("instanceID")
	public UUID instanceID() {
		return UUID.randomUUID();
	}
	
}
