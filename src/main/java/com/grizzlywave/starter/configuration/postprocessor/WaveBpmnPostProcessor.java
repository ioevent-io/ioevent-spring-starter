package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventBpmnPart;
import com.grizzlywave.starter.listener.Listener;
import com.grizzlywave.starter.listener.ListenerCreator;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * class configuration for Wave Bpmn Part Creation using Bean Post Processor
 **/
@Slf4j
@Configuration
public class WaveBpmnPostProcessor implements BeanPostProcessor, WavePostProcessors {
	public static Boolean listenerCreatorStatus=true;

	@Autowired
	private WaveProperties waveProperties;

	@Autowired
	private List<IOEventBpmnPart> iobpmnlist;
	@Autowired
	private ListenerCreator ListenerCreator;
	@Autowired
	private List<Listener> listeners;

	@Autowired
	private IOEventService ioEventService;
	/**
	 * method post processor  before initialization,
	 * @param bean for the bean,
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
	 * method post processor  after initialization,
	 * @param bean for the bean,
	 * @param beanName for the bean name,
	 **/
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * process method to check for annotations in the bean and create the Bpmn parts,
	 * @param bean for the bean,
	 * @param beanName for the bean name,
	 **/
	@Override
	public void process(Object bean, String beanName) throws Throwable {

		for (Method method : bean.getClass().getMethods()) {

			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);

				for (IOEvent ioEvent : ioEvents) {
					
					if (ioEvent.startEvent().key().isEmpty()) {
					
						for (String topicName : ioEventService.getSourceTopic(ioEvent)) {
						
							if (!ListenerExist(topicName, bean, method, ioEvent)) {
								synchronized (method) {
									ListenerCreator.createListener(bean, method, ioEvent,
											waveProperties.getPrefix() + topicName, waveProperties.getGroup_id(),Thread.currentThread());
									method.wait();
								}
							}
						}
					}
					String generateID= ioEventService.generateID(ioEvent);
					iobpmnlist.add(this.ioEventBpmnPart(ioEvent, bean.getClass().getName(), generateID, method.getName()));
				
			}
		}
	}

	/** check if the listener already exist
	 * @param bean for the bean,
	 * @param topicName for the topic name,
	 * @param method for the method information,
	 * @param ioEvent for the ioEvent annotation info,
	 * @return boolean true if  the listener exist else false,
	 * */
	public boolean ListenerExist(String topicName, Object bean, Method method, IOEvent ioEvent)
			 {
		for (Listener listener : listeners) {
			if (listener != null) {
				String t = listener.getTopic();
				if (t.equals(waveProperties.getPrefix() + topicName)) {

					listener.addBeanMethod(new BeanMethodPair(bean, method, ioEvent));
					
					return true;
				}
			}
		}
		return false;
	}
	/** methods to create IOEvent BPMN Parts from annotations
	 * @param ioEvent for the ioEvent annotation info,
	 * @param className for the class which include the method,
	 * @param partID for the part ID,
	 * @param methodName  for the method name,
	 * **/
	public IOEventBpmnPart ioEventBpmnPart(IOEvent ioEvent, String className, String partID, String methodName) {
		String processName = "";
		if (!ioEvent.startEvent().key().isEmpty()) {
			processName = ioEvent.startEvent().key();
		} else if (!ioEvent.endEvent().key().isEmpty()) {
			processName = ioEvent.endEvent().key();
		}
		IOEventBpmnPart ioeventBpmnPart = new IOEventBpmnPart(ioEvent, partID, processName,
				ioEventService.getIOEventType(ioEvent), ioEvent.name(), className, methodName);
		return ioeventBpmnPart;
	}

}
