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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.annotations.InputEvent;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventBpmnPart;
import com.ioevent.starter.domain.IOEventExceptionInformation;
import com.ioevent.starter.domain.IOEventGatwayInformation;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.listener.Listener;
import com.ioevent.starter.listener.ListenerCreator;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * class configuration for IOEvent Bpmn Part Creation using Bean Post Processor
 **/
@Slf4j
@Configuration
public class IOEventBpmnPostProcessor implements BeanPostProcessor, IOEventPostProcessors {

	@Value("${spring.application.name}")
	private String appName;
	@Value("#{'${spring.kafka.consumer.group-id:${ioevent.group_id:${spring.application.name:ioevent_default_groupid}}}'}")
	private String kafkaGroupid;
	@Autowired
	private IOEventProperties iOEventProperties;

	@Autowired
	private List<IOEventBpmnPart> iobpmnlist;
	@Autowired
	private ListenerCreator listenerCreator;
	@Autowired
	private List<Listener> listeners;
	@Autowired
	private Set<String> apiKeys;
	@Autowired
	private AdminClient client;
	@Autowired
	private IOEventService ioEventService;

	/**
	 * method post processor before initialization,
	 * 
	 * @param bean     for the bean,
	 * @param beanName for the bean name,
	 * @return A bean Object ,
	 **/
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		try {

			this.process(bean, beanName);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return bean;
	}

	/**
	 * method post processor after initialization,
	 * 
	 * @param bean     for the bean,
	 * @param beanName for the bean name,
	 **/
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * process method to check for annotations in the bean and create the Bpmn parts
	 * 
	 * @param bean     for the bean,
	 * @param beanName for the bean name,
	 * @throws Exception
	 **/
	@Override
	public void process(Object bean, String beanName) throws Exception {
		IOFlow ioFlow = bean.getClass().getAnnotation(IOFlow.class);
		addApikey(apiKeys, ioFlow, iOEventProperties);
		for (Method method : bean.getClass().getMethods()) {

			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);
			for (IOEvent ioEvent : ioEvents) {
				checkMethodValidation(ioFlow, ioEvent, method);
				if (needListener(ioEvent)) {

					for (String topicName : ioEventService.getInputTopic(ioEvent, ioFlow)) {
						if (!listenerExist(topicName, bean, method, ioEvent)) {
							DescribeTopicsResult describeTopics = client
									.describeTopics(Collections.singleton(iOEventProperties.getPrefix() + topicName));
							int partitionNumber = describeTopics.all().get()
									.get(iOEventProperties.getPrefix() + topicName).partitions().size();
							for (int i = 0; i < (partitionNumber / 2) + 1; i++) {
								synchronized (method) {
									Thread listenerThread = new Thread() {
										@Override
										public void run() {
											try {
												listenerCreator.createListener(bean, method, ioEvent,
														iOEventProperties.getPrefix() + topicName, kafkaGroupid,
														Thread.currentThread());
											} catch (Throwable e) {
												log.error("Listener creation failed   !!!");
											}
										}
									};
									listenerThread.start();

									method.wait();

								}
							}

						}
					}
				}
				String methodReturnType = ioEventService.getMethodReturnType(method); 
				String generateID = ioEventService.generateID(ioEvent);
				iobpmnlist.add(createIOEventBpmnPart(ioEvent, ioFlow, bean.getClass().getName(), generateID,
						method.toGenericString(),methodReturnType,iOEventProperties.getPrefix()));

			}
		}
	}

	@Autowired
	ApplicationContext applicationContext;

	public void checkMethodValidation(IOFlow ioFlow, IOEvent ioEvent, Method method) {
		try {
			ioEventService.ioflowExistValidation(ioFlow);
			ioEventService.ioeventKeyValidation(ioEvent);
			ioEventService.gatewayValidation(ioEvent, method);
			ioEventService.startAndEndvalidation(ioEvent, method);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			SpringApplication.exit(applicationContext, () -> 0);
			System.exit(0);

		}

	}

	public boolean needListener(IOEvent ioEvent) {

		if (((StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value()))
				&& (ioEvent.input().length != 0)) || (ioEvent.gatewayInput().input().length != 0)) {
			for (InputEvent input : ioEvent.input()) {
				if (!StringUtils.isBlank(input.key() + input.value())) {
					return true;
				}
			}
			for (InputEvent input : ioEvent.gatewayInput().input()) {
				if (!StringUtils.isBlank(input.key() + input.value())) {
					return true;
				}
			}

		}
		return false;
	}

	public void addApikey(Set<String> apiKeys, IOFlow ioFlow, IOEventProperties iOEventProperties) {
		apiKeys.add(iOEventProperties.getApikey());
		if (!Objects.isNull(ioFlow)) {
			if (StringUtils.isNotBlank(ioFlow.apiKey())) {
				apiKeys.add(ioFlow.apiKey());
			}
		}
	}

	/**
	 * check if the listener already exist,
	 * 
	 * @param bean      for the bean,
	 * @param topicName for the topic name,
	 * @param method    for the method information,
	 * @param ioEvent   for the ioEvent annotation info,
	 * @return boolean true if the listener exist else false,
	 **/
	public boolean listenerExist(String topicName, Object bean, Method method, IOEvent ioEvent) {
		boolean isExist = false;
		for (Listener listener : listeners) {
			if (listener != null) {
				String t = listener.getTopic();
				if (t.equals(iOEventProperties.getPrefix() + topicName)) {

					listener.addBeanMethod(new BeanMethodPair(bean, method, ioEvent));

					isExist = true;
				}
			}
		}
		return isExist;
	}

	/**
	 * methods to create IOEvent BPMN Parts from annotations
	 * 
	 * @param ioEvent    for the ioEvent annotation info,
	 * @param ioFlow     for the ioFlow annotation info ,
	 * @param className  for the class which include the method,
	 * @param partID     for the part ID,
	 * @param methodName for the method name,
	 * @param string 
	 * @param string 
	 **/
	public IOEventBpmnPart createIOEventBpmnPart(IOEvent ioEvent, IOFlow ioFlow, String className, String partID,
			String methodName,String methodReturnType, String topicPrefix) {
		String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);

		if (!StringUtils.isBlank(ioEvent.exception().endEvent().value())) {
			IOEventBpmnPart errorEnd = new IOEventBpmnPart();
			errorEnd.setApiKey(apiKey);
			errorEnd.setId("ErrorEnd_" + partID);
			errorEnd.setMethodQualifiedName("ErrorEnd of " + methodName);
			errorEnd.setStepName(ioEvent.exception().endEvent().value());
			errorEnd.setWorkflow(processName);
			errorEnd.setIoEventType(IOEventType.ERROR_END);
			errorEnd.setIoAppName(appName);

			HashMap<String, String> input = new HashMap<>();
			input.put(ioEvent.exception().endEvent().value(), ioEvent.topic());
			errorEnd.setInputEvent(input);

			errorEnd.setIoeventGatway(new IOEventGatwayInformation());
			IOEventExceptionInformation ioEventException = new IOEventExceptionInformation();
			if (!StringUtils.isBlank(ioEvent.exception().exception().toString())) {
				ioEventException.setErrorType(Arrays.toString(ioEvent.exception().exception()));
			}
			errorEnd.setIoeventException(ioEventException);

			errorEnd.setOutputEvent(new HashMap<>());

			iobpmnlist.add(errorEnd);
		}

		return new IOEventBpmnPart(ioEvent,ioFlow, partID, apiKey, appName, processName,
				ioEventService.getIOEventType(ioEvent), ioEvent.key(), methodName,methodReturnType,topicPrefix);

	}

}
