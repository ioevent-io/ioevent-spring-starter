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



package com.ioevent.starter.configuration;

import java.util.ArrayList;
import java.util.Collection;
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
import com.ioevent.starter.configuration.aspect.v2.IOExceptionHandlingAspect;
import com.ioevent.starter.configuration.context.AppContext;
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
import com.ioevent.starter.service.IOEventMessageBuilderService;
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
	 * method for processing parallel events from the ioevent-parallel-gateway-events topic using kafka stream,
	 * 
	 * @param builder type of StreamsBuilder,
	 */
	@Autowired
	public void processKStream(final StreamsBuilder builder) {

		Gson gson = new Gson();

		KStream<String, String> kstream = builder
				.stream("ioevent-parallel-gateway-events", Consumed.with(Serdes.String(), Serdes.String()))
				.map(KeyValue::new).filter((k, v) -> {
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
							.flatMap(Collection::stream).distinct().collect(Collectors.toList());
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
				}).toStream().to("ioevent-parallel-gateway-aggregation", Produced.with(Serdes.String(), Serdes.String()));

	}
	@ConditionalOnMissingBean
	@Bean
	public IOEventParrallelListener ioEventParrallelListener() {
		return new IOEventParrallelListener();
	}

	@Bean
	public AppContext appContext() {
		return new AppContext();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEventProperties ioEventProperties() {
		return new IOEventProperties();
	}

	@Bean
	public TopicServices topicServices() {
		return new TopicServices();
	}
	@ConditionalOnMissingBean
	@Bean
	public RecordsHandler recordsHandler() {
		return new RecordsHandler();
	}

	@Bean
	public ListenerCreator listenerCreator() {
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

	@ConditionalOnMissingBean
	@Bean
	public IOEventTopicBeanPostProcessor ioEventTopicBeanPostProcessor() {
		return new IOEventTopicBeanPostProcessor();
	}

	@ConditionalOnMissingBean
@Bean
	public IOEventBpmnPostProcessor ioEventBpmnPostProcessor() {
		return new IOEventBpmnPostProcessor();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEventStartAspect ioEventStartAspect() {
		return new IOEventStartAspect();
	}

	@ConditionalOnMissingBean
@Bean
	public IOEventTransitionAspect ioEventTransitionAspect() {
		return new IOEventTransitionAspect();
	}	
	@ConditionalOnMissingBean
	@Bean
	public IOExceptionHandlingAspect ioExceptionHandlingAspect() {
		return new IOExceptionHandlingAspect();
	}
	

	@ConditionalOnMissingBean
	@Bean
	public IOEventEndAspect ioEventEndAspect() {
		return new IOEventEndAspect();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEvenImplicitTaskAspect ioEvenImplicitTaskAspect() {
		return new IOEvenImplicitTaskAspect();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEventController ioEventController() {
		return new IOEventController();
	}

	

	@Bean("iobpmnlist")
	public List<IOEventBpmnPart> iobpmnlist() {
		return new LinkedList<>();
	}
	@Bean("ioTopics")
	public Set<String> ioTopics() {
		return new HashSet<>();
	}
	
	@Bean("apiKeys")
	public Set<String> apiKeys() {
		return new HashSet<>();
	}
	@Bean("listeners")
	public List<Listener> listeners() {
		return new ArrayList<>();
	}

	@ConditionalOnMissingBean
	@Bean
	public IOEventService ioEventService() {
		return new IOEventService();
	}
	@ConditionalOnMissingBean
	@Bean
	public IOEventMessageBuilderService ioeventMessageBuilderService() {
		return new IOEventMessageBuilderService();
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
