package com.grizzlywave.starter.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.grizzlywave.starter.configuration.aspect.LogAspect;
import com.grizzlywave.starter.configuration.aspect.WaveEndAspect;
import com.grizzlywave.starter.configuration.aspect.WaveInitAspect;
import com.grizzlywave.starter.configuration.aspect.WaveTransitionAspect;
import com.grizzlywave.starter.configuration.kafka.KafkaConfig;
import com.grizzlywave.starter.configuration.postprocessor.WaveBpmnPostProcessor;
import com.grizzlywave.starter.configuration.postprocessor.WaveTopicBeanPostProcessor;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.controller.WaveController;
import com.grizzlywave.starter.listener.ListenerCreator;
import com.grizzlywave.starter.model.WaveBpmnPart;
import com.grizzlywave.starter.service.LogAnnotaionService;
import com.grizzlywave.starter.service.TopicServices;

/**
 * class for wave configuration which contains all configurations needed by a
 * project which use GrizzlyWave
 **/
@EnableKafka
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@EnableAsync
@Import({ KafkaConfig.class })
public class WaveConfiguration {

	
	@Bean
	public com.grizzlywave.starter.configuration.context.AppContext AppContext() {
		return new com.grizzlywave.starter.configuration.context.AppContext();
	}
	@ConditionalOnMissingBean
	@Bean
	public LogAnnotaionService LogAnnotaionService() {
		return new LogAnnotaionService();
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
	public LogAspect LogAspect() {
		return new LogAspect();
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
	@ConditionalOnMissingBean
	@Bean
	public WaveController WaveController() {
		return new WaveController();
	}

	@Bean("bpmnlist")
	public List<WaveBpmnPart> bpmnlist() {
		return new ArrayList<WaveBpmnPart>();
	}
	

}
