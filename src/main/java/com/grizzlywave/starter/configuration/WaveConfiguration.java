package com.grizzlywave.starter.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.grizzlywave.starter.configuration.aspect.AnnotationAspect;
import com.grizzlywave.starter.configuration.kafka.KafkaConfig;
import com.grizzlywave.starter.configuration.postprocessor.WaveBpmnPostProcessor;
import com.grizzlywave.starter.configuration.postprocessor.WaveTopicBeanPostProcessor;
import com.grizzlywave.starter.controller.WaveController;
import com.grizzlywave.starter.service.TopicServices;

/**
 * class for wave configuration which contains all configurations needed by a
 * project which use GrizzlyWave
 **/
@Configuration
@Import({ KafkaConfig.class })
public class WaveConfiguration {
	@Bean
	@ConditionalOnMissingBean
	WaveConfigProperties WaveConfigProperties() {
		return new WaveConfigProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public TopicServices TopicServices() {
		return new TopicServices();
	}

	@Bean
	@ConditionalOnMissingBean
	public WaveTopicBeanPostProcessor WaveTopicBeanPostProcessor() {
		return new WaveTopicBeanPostProcessor();
	}

	@Bean
	@ConditionalOnMissingBean
	public WaveBpmnPostProcessor WaveBpmnPostProcessor() {
		return new WaveBpmnPostProcessor();
	}

	@Bean
	@ConditionalOnMissingBean
	public AnnotationAspect AnnotationAspect() {
		return new AnnotationAspect();
	}

	@Bean
	@ConditionalOnMissingBean
	public WaveController WaveController() {
		return new WaveController();
	}

	@Bean("bpmnlist")
	public List<Map<String, Object>> bpmnlist() {
		return new ArrayList<Map<String, Object>>();
	}
}
