package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.lang.Nullable;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.ParallelEventInfo;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.TopicServices;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;

/**
 * Class Configuration for Wave Topic creation using Bean Post Processor ,
 * create topics mentioned in property Grizzly-wave topic-names , if the auto
 * creation property is enable : this configuration will create topics mentioned
 * in all annotations if not it will generate an exception
 **/
@Slf4j
@Primary
@Configuration
public class WaveTopicBeanPostProcessor implements DestructionAwareBeanPostProcessor, WavePostProcessors {


	@Autowired
	private WaveProperties waveProperties;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private AdminClient client;

	@Autowired
	private IOEventService ioEventService;
	
	
	/** BeanPostProcessor method to execute Before Bean Initialization */

	@Nullable
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		try {
			this.process(bean, beanName);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			// log.er
		} catch (Exception e) {
			e.printStackTrace();
			SpringApplication.exit(context);
		}
		return bean;
	}
	
	/** BeanPostProcessor method to execute After Bean Initialization */
	@Nullable
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		
		if (bean instanceof TopicServices) {
			((TopicServices) bean).createTopic("ParallelEventTopic","",waveProperties.getTopicReplication());
			((TopicServices) bean).createTopic("resultTopic","",waveProperties.getTopicReplication());

			if (waveProperties.getTopic_names()!=null) {
				waveProperties.getTopic_names().stream()
				.forEach(x -> ((TopicServices) bean).createTopic(x, waveProperties.getPrefix(),waveProperties.getTopicReplication()));
		log.info("topics created");
			}
			ioEventService.sendParallelEventInfo(new WaveParallelEventInformation());

		}
		return bean;
	}


	/**
	 * Process Method take the Bean as a parameter collect all Grizzly_Wave custom
	 * annotations Verifies if the topics uses in these annotations are already
	 * exist if not then create all them in condition that the property
	 * auto_create_topic is true
	 **/


	@Override
	public void process(Object bean, String beanName) throws Exception {
		for (Method method : bean.getClass().getMethods()) {
			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);
		 
			if (ioEvents.length != 0) {
				for (IOEvent ioEvent : ioEvents) {
					for (String topicName : ioEventService.getTopics(ioEvent)) {
						if (!topicExist(topicName)) {

							if (waveProperties.getAuto_create_topic()) {
								log.info("creating topic : " + topicName);

								//TopicBuilder.name(waveProperties.getPrefix()+ topicName).partitions(1).replicas((short) 1).build();
								client.createTopics(Arrays
										.asList(new NewTopic(waveProperties.getPrefix() + topicName, 12,Short.valueOf(waveProperties.getTopicReplication()))));

							} else
								throw new Exception(
										"Topics doesn't Exist : You must Create them By Adding topics Name in Properties");

						}
					}
				}
			}

		}

	}

	private boolean topicExist(String topic) throws InterruptedException, ExecutionException {
		if ((client.listTopics().names().get().stream()
				.anyMatch(topicName -> topicName.equalsIgnoreCase(waveProperties.getPrefix() + topic)))) {
			log.info("topic : "+waveProperties.getPrefix()+topic +"alreay exist");
			return true;
		} else {
			return false;
		}
	}

	
	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		// TODO Auto-generated method stub

	}
}
