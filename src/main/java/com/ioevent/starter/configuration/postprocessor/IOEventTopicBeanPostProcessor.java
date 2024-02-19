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

package com.ioevent.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.service.IOEventService;
import com.ioevent.starter.service.TopicServices;

import lombok.extern.slf4j.Slf4j;

/**
 * Class Configuration for IOEvent Topic creation using Bean Post Processor ,
 * create topics mentioned in property IOEvent topic-names , if the auto
 * creation property is enable : this configuration will create topics mentioned
 * in all annotations if not it will generate an exception
 **/
@Slf4j
@Primary
@Configuration
public class IOEventTopicBeanPostProcessor implements DestructionAwareBeanPostProcessor, IOEventPostProcessors {

	@Value("${spring.kafka.streams.replication-factor:1}")
	private String replicationFactor;

	@Autowired
	private IOEventProperties iOEventProperties;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private AdminClient client;
	@Autowired
	private Set<String> ioTopics;
	@Autowired
	private IOEventService ioEventService;
	@Value("${spring.application.name}")
	private String applicationName;
	@Autowired
	private IOEventProperties ioeventProperties;

	/**
	 * BeanPostProcessor method to execute Before Bean Initialisation ,
	 * 
	 * @param bean     for the bean Object,
	 * @param beanName for the bean name,
	 * @return bean Object,
	 **/

	@Nullable
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		try {
			this.process(bean, beanName);

		} catch (Exception e) {
			log.error(e.getMessage());
			SpringApplication.exit(context);
		}
		return bean;
	}

	/**
	 * BeanPostProcessor method to execute After Bean Initialisation
	 * 
	 * @param bean     for the bean Object,
	 * @param beanName for the bean name,
	 * @return bean Object,
	 **/
	@Nullable
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		if (bean instanceof TopicServices) {
			((TopicServices) bean).createTopic("ioevent-parallel-gateway-events", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic("ioevent-parallel-gateway-aggregation", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic("ioevent-apps", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic("ioevent-implicit-topic", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic("ioevent-timer", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic("ioevent-timer-execute", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic("ioevent-message-events", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic("ioevent-event-message-aggregation", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic(ioeventProperties.getPrefix()+ioEventService.getUserTaskTopicName(applicationName)+"_"+"ioevent-user-task", "", replicationFactor,
					iOEventProperties.getTopic_partition());
			((TopicServices) bean).createTopic(ioeventProperties.getPrefix()+ioEventService.getUserTaskTopicName(applicationName)+"_"+"ioevent-user-task-Response", "", replicationFactor,
					iOEventProperties.getTopic_partition());

			ioTopics.add(iOEventProperties.getPrefix()+ioEventService.getUserTaskTopicName(applicationName)+"_"+"ioevent-user-task");
			
			if (iOEventProperties.getTopic_names() != null) {
				iOEventProperties.getTopic_names().stream().forEach(x -> ((TopicServices) bean).createTopic(x,
						iOEventProperties.getPrefix(), replicationFactor, iOEventProperties.getTopic_partition()));
				log.info("topics created");
				iOEventProperties.getTopic_names().stream()
						.forEach(x -> ioTopics.add(iOEventProperties.getPrefix() + x));
			}

		}
		return bean;
	}

	/**
	 * Process Method take the Bean as a parameter collect all IOEvent custom
	 * annotations Verifies if the topics uses in these annotations are already
	 * exist if not then create all them in condition that the property
	 * auto_create_topic is true,
	 * 
	 * @param bean     for the bean Object,
	 * @param beanName for the bean name,
	 * @throws Exception
	 **/

	@Override
	public void process(Object bean, String beanName) throws Exception {
		Arrays.stream(bean.getClass().getAnnotationsByType(IOFlow.class)).forEach(ioflow -> {
			try {
				createIOFlowTopic(ioflow);
			} catch (NumberFormatException | InterruptedException | ExecutionException e) {
				log.info("failed to create ioflow topic !");

			}
		});

		for (Method method : bean.getClass().getMethods()) {
			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);
			if (ioEvents.length != 0) {
				for (IOEvent ioEvent : ioEvents) {
					ioEventService.getTopics(ioEvent).stream()
							.forEach(x -> ioTopics.add(iOEventProperties.getPrefix() + x));
					for (String topicName : ioEventService.getTopics(ioEvent)) {
						if (!topicExist(topicName)) {

							if (iOEventProperties.getAuto_create_topic()) {
								log.info("creating topic : " + topicName);

								// TopicBuilder.name(ioeventProperties.getPrefix()+
								// topicName).partitions(1).replicas((short) 1).build();
								client.createTopics(Arrays.asList(new NewTopic(
										iOEventProperties.getPrefix() + topicName,
										iOEventProperties.getTopic_partition(), Short.valueOf(replicationFactor))));

							} else
								throw new Exception(
										"Topics doesn't Exist : You must Create them By Adding topics Name in Properties");

						}
					}
				}
			}

		}

	}

	/**
	 * method that create topics of IOFlow annotation ,
	 * 
	 * @param ioFlow for the IOFlow annotation,
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws NumberFormatException
	 */
	private void createIOFlowTopic(IOFlow ioFlow)
			throws NumberFormatException, InterruptedException, ExecutionException {
		if (!StringUtils.isBlank(ioFlow.topic())) {
			ioTopics.add(iOEventProperties.getPrefix() + ioFlow.topic());
			if (!topicExist(ioFlow.topic())) {
				if (Boolean.TRUE.equals(iOEventProperties.getAuto_create_topic())) {
					log.info("creating topic : " + ioFlow.topic());
					client.createTopics(Arrays.asList(new NewTopic(iOEventProperties.getPrefix() + ioFlow.topic(),
							iOEventProperties.getTopic_partition(), Short.valueOf(replicationFactor))));
				}
			}
		}
	}

	/**
	 * method that check if topic exists or not,
	 * 
	 * @param topic for topic name,
	 * @return boolean (true or false),
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public boolean topicExist(String topic) throws InterruptedException, ExecutionException {
		if ((client.listTopics().names().get().stream()
				.anyMatch(topicName -> topicName.equalsIgnoreCase(iOEventProperties.getPrefix() + topic)))) {
			
			return true;
		} else {
			return false;
		}
	}

	/**
	 * BeanPostProcessor method to execute After Bean Destruction
	 * 
	 * @param bean     for the bean Object,
	 * @param beanName for the bean name,
	 */
	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
   // Execute After Bean Destruction
 }
}
