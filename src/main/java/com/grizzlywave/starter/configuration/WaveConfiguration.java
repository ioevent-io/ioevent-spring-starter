package com.grizzlywave.starter.configuration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.grizzlywave.starter.configuration.aspect.v2.IOEventEndAspect;
import com.grizzlywave.starter.configuration.aspect.v2.IOEventStartAspect;
import com.grizzlywave.starter.configuration.aspect.v2.IOEventTransitionAspect;
import com.grizzlywave.starter.configuration.eureka.EurekaConfig;
import com.grizzlywave.starter.configuration.kafka.KafkaConfig;
import com.grizzlywave.starter.configuration.postprocessor.WaveBpmnPostProcessor;
import com.grizzlywave.starter.configuration.postprocessor.WaveTopicBeanPostProcessor;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.controller.WaveController;
import com.grizzlywave.starter.domain.IOEventBpmnPart;
import com.grizzlywave.starter.domain.WaveBpmnPart;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;
import com.grizzlywave.starter.handler.RecordsHandler;
import com.grizzlywave.starter.listener.Listener;
import com.grizzlywave.starter.listener.ListenerCreator;
import com.grizzlywave.starter.listener.WaveParrallelListener;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.TopicServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * class for wave configuration which contains all configurations needed by a
 * project which use GrizzlyWave
 **/
@Slf4j
@EnableKafka
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableKafkaStreams
@EnableAsync
@Import({ KafkaConfig.class, EurekaConfig.class })
@Service
@RequiredArgsConstructor
public class WaveConfiguration {


	ObjectMapper mapper = new ObjectMapper();

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	public void processKStream(final StreamsBuilder builder) {

		Gson gson = new Gson();

		KStream<String, String> kstream = builder
				.stream("ParallelEventTopic", Consumed.with(Serdes.String(), Serdes.String()))
				.map((k, v) -> new KeyValue<>(k, v)).filter((k, v) -> {
					WaveParallelEventInformation value = gson.fromJson(v, WaveParallelEventInformation.class);
					return appName.equals(value.getHeaders().get("AppName"));
				});
		kstream.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
				.aggregate(() -> "", (key, value, aggregateValue) -> {
					WaveParallelEventInformation currentValue = gson.fromJson(value,
							WaveParallelEventInformation.class);
					WaveParallelEventInformation updatedValue;
					if (!aggregateValue.isBlank()) {
						updatedValue = gson.fromJson(aggregateValue, WaveParallelEventInformation.class);
					} else {
						updatedValue = currentValue;
					}
					List<String> updatedTargetList = Stream.of(currentValue.getTargetsArrived(), updatedValue.getTargetsArrived())
							.flatMap(x -> x.stream()).distinct().collect(Collectors.toList());
					Map<String, String> updatedHeaders = Stream.of(currentValue.getHeaders(), updatedValue.getHeaders())
							.flatMap(map -> map.entrySet().stream())
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
					updatedValue.setTargetsArrived(updatedTargetList);
					updatedValue.setHeaders(updatedHeaders);
					aggregateValue = gson.toJson(updatedValue);
					return aggregateValue;
				}).toStream().to("resultTopic", Produced.with(Serdes.String(), Serdes.String()));
		
	}

	@Bean
	public WaveParrallelListener WaveParrallelListener() {
		return new WaveParrallelListener();
	}

	@Bean
	public com.grizzlywave.starter.configuration.context.AppContext AppContext() {
		return new com.grizzlywave.starter.configuration.context.AppContext();
	}

	@ConditionalOnMissingBean
	@Bean
	public WaveProperties WaveProperties() {
		return new WaveProperties();
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
	public WaveTopicBeanPostProcessor WaveTopicBeanPostProcessor() {
		return new WaveTopicBeanPostProcessor();
	}

	@Bean
	public WaveBpmnPostProcessor WaveBpmnPostProcessor() {
		return new WaveBpmnPostProcessor();
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

	@ConditionalOnMissingBean
	@Bean
	public WaveController WaveController() {
		return new WaveController();
	}

	@Bean("bpmnlist")
	public List<WaveBpmnPart> bpmnlist() {
		return new ArrayList<>();
	}

	@Bean("iobpmnlist")
	public List<IOEventBpmnPart> iobpmnlist() {
		return new LinkedList<>();
	}

	@Bean("listeners")
	public List<Listener> listeners() {
		return new ArrayList<>();
	}

	@Bean
	public IOEventService IOEventService() {
		return new IOEventService();
	}
}
