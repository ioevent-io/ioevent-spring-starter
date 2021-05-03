package com.grizzlywave.starter.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

import com.grizzlywave.starter.configuration.aspect.WaveEndAspect;
import com.grizzlywave.starter.configuration.aspect.WaveInitAspect;
import com.grizzlywave.starter.configuration.aspect.WaveTransitionAspect;
import com.grizzlywave.starter.configuration.kafka.KafkaConfig;
import com.grizzlywave.starter.configuration.postprocessor.WaveBpmnPostProcessor;
import com.grizzlywave.starter.configuration.postprocessor.WaveTopicBeanPostProcessor;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.controller.WaveController;
import com.grizzlywave.starter.service.TopicServices;

/**
 * class for wave configuration which contains all configurations needed by a
 * project which use GrizzlyWave
 **/
@EnableKafka
@EnableAsync
@Configuration
@Import({ KafkaConfig.class })
public class WaveConfiguration {
	@Bean
	WaveProperties WaveProperties() {
		return new WaveProperties();
	}

	@Bean
	public TopicServices TopicServices() {
		return new TopicServices();
	}

	@Bean
	public WaveTopicBeanPostProcessor WaveTopicBeanPostProcessor() {
		return new WaveTopicBeanPostProcessor();
	}

	@Bean
	public WaveBpmnPostProcessor WaveBpmnPostProcessor() {
		return new WaveBpmnPostProcessor();
	}

	@Bean
	public WaveInitAspect WaveInitAspect() {
		return new WaveInitAspect();
	}
	@Bean
	public WaveTransitionAspect WaveTransitionAspect() {
		return new WaveTransitionAspect();
	}
	
	@Bean
	public WaveEndAspect WaveEndAspect() {
		return new WaveEndAspect();
	}

	@Bean
	public WaveController WaveController() {
		return new WaveController();
	}

	@Bean("bpmnlist")
	public List<Map<String, Object>> bpmnlist() {
		return new ArrayList<Map<String, Object>>();
	}
	 

}
