package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

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

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * process method to check for annotations in the bean and create the Bpmn parts
	 **/
	@Override
	public void process(Object bean, String beanName) throws Throwable {

		for (Method method : bean.getClass().getMethods()) {

			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);

			if (ioEvents.length != 0) {
				for (IOEvent ioEvent : ioEvents) {
					if (ioEvent.startEvent().key().isEmpty()) {
						for (String topicName : ioEventService.getSourceTopic(ioEvent)) {
							if (!ListenerExist(topicName, bean, method, ioEvent)) {
								ListenerCreator.createListener(bean, method, ioEvent,
										waveProperties.getPrefix() + topicName, waveProperties.getGroup_id());
								Thread.sleep(1000);
							}
						}
					}

					UUID uuid = UUID.randomUUID();
					iobpmnlist.add(this.ioEventBpmnPart(ioEvent, bean.getClass().getName(), uuid, method.getName()));
				}
			}
		}
	}

	/** check if the listener already exist */
	private boolean ListenerExist(String topicName, Object bean, Method method, IOEvent ioEvent)
			throws InterruptedException {
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

	

	/** methods to create IOEvent BPMN Parts from annotations **/
	private IOEventBpmnPart ioEventBpmnPart(IOEvent ioEvent, String className, UUID uuid, String methodName) {
		String processName = "";
		if (!ioEvent.startEvent().key().equals("")) {
			processName = ioEvent.startEvent().key();
		} else if (!ioEvent.endEvent().key().equals("")) {
			processName = ioEvent.endEvent().key();
		}
		IOEventBpmnPart ioeventBpmnPart = new IOEventBpmnPart(ioEvent, uuid, processName,
				ioEventService.getIOEventType(ioEvent), ioEvent.name(), className, methodName);
		return ioeventBpmnPart;
	}

}
