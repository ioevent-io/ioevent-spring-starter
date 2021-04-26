package com.grizzlywave.grizzlywavestarter;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.event.EventListener;

import com.grizzlywave.grizzlywavestarter.configuration.WaveBpmnPostProcessor;
import com.grizzlywave.grizzlywavestarter.configuration.WaveConfigProperties;
import com.grizzlywave.grizzlywavestarter.model.Order;
import com.grizzlywave.grizzlywavestarter.service.TopicServices;
import com.grizzlywave.grizzlywavestarter.service.waveInitAnnotation;

/**
 * Grizzly Wave Starter Main Class
 **/
@SpringBootApplication
@EnableEurekaClient
@EnableConfigurationProperties(WaveConfigProperties.class)
public class GrizzlyWaveStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
	}

	private static final Logger log = LoggerFactory.getLogger(GrizzlyWaveStarterApplication.class);
	@Autowired
	waveInitAnnotation waveInitAnnotation;
	@Autowired
	TopicServices topicService;

	@EventListener(ApplicationReadyEvent.class)
	public void shouldLogWaveInitAnnotationMethod()
			throws IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException {

		log.info(waveInitAnnotation.initOrder(new Order(2, 2, 200)).toString());
		log.info("list of topic :" + topicService.getAllTopic().toString());
		log.info("BPMN PART :"+WaveBpmnPostProcessor.bpmnPart);
	}
	

}
