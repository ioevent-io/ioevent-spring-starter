package com.ioevent.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import com.ioevent.starter.annotations.IOEvent;
import com.ioevent.starter.annotations.IOFlow;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventBpmnPart;
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
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
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
	 **/
	@Override
	public void process(Object bean, String beanName) throws Throwable {
		IOFlow ioFlow = bean.getClass().getAnnotation(IOFlow.class);
		addApikey(apiKeys, ioFlow,iOEventProperties);
		for (Method method : bean.getClass().getMethods()) {

			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);

			for (IOEvent ioEvent : ioEvents) {

				if (StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())) {

					for (String topicName : ioEventService.getSourceTopic(ioEvent, ioFlow)) {
						if (!listenerExist(topicName, bean, method, ioEvent)) {
							synchronized (method) {
								Thread listenerThread = new Thread() {
									@Override
									public void run() {
										try {
											listenerCreator.createListener(bean, method, ioEvent,
													iOEventProperties.getPrefix() + topicName,
													iOEventProperties.getGroup_id(), Thread.currentThread());
										} catch (Throwable e) {
											log.error("Listener failed   !!!");
										}
									}
								};
								listenerThread.start();

								method.wait();
							}
						}
					}
				}
				String generateID = ioEventService.generateID(ioEvent);
				iobpmnlist.add(createIOEventBpmnPart(ioEvent, ioFlow, bean.getClass().getName(), generateID,
						method.getName()));

			}
		}
	}

	public void addApikey(Set<String> apiKeys, IOFlow ioFlow, IOEventProperties iOEventProperties) {
		apiKeys.add(iOEventProperties.getApikey()) ;
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
		for (Listener listener : listeners) {
			if (listener != null) {
				String t = listener.getTopic();
				if (t.equals(iOEventProperties.getPrefix() + topicName)) {

					listener.addBeanMethod(new BeanMethodPair(bean, method, ioEvent));

					return true;
				}
			}
		}
		return false;
	}

	/**
	 * methods to create IOEvent BPMN Parts from annotations
	 * 
	 * @param ioEvent    for the ioEvent annotation info,
	 * @param ioFlow     for the ioFlow annotation info ,
	 * @param className  for the class which include the method,
	 * @param partID     for the part ID,
	 * @param methodName for the method name,
	 **/
	public IOEventBpmnPart createIOEventBpmnPart(IOEvent ioEvent, IOFlow ioFlow, String className, String partID,
			String methodName) {
		String processName = ioEventService.getProcessName(ioEvent, ioFlow, "");
		String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
		return new IOEventBpmnPart(ioEvent, partID, apiKey, processName, ioEventService.getIOEventType(ioEvent),
				ioEvent.key(), className, methodName);

	}

}
