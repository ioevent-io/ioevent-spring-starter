package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.starter.annotations.WaveEnd;
import com.grizzlywave.starter.annotations.WaveInit;
import com.grizzlywave.starter.annotations.WaveTransition;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventBpmnPart;
import com.grizzlywave.starter.domain.WaveBpmnPart;
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
	private List<WaveBpmnPart> bpmnlist;

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

			this.process(bean, this.getworkFlow(bean, bean.getClass().getAnnotationsByType(WaveWorkFlow.class)));
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
	public void process(Object bean, String workFlow) throws Throwable {

		for (Method method : bean.getClass().getMethods()) {
			WaveInit[] waveInit = method.getAnnotationsByType(WaveInit.class);
			WaveTransition[] waveTransition = method.getAnnotationsByType(WaveTransition.class);
			WaveEnd[] waveEnds = method.getAnnotationsByType(WaveEnd.class);
			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);
			if (waveInit.length != 0)
				for (WaveInit x : waveInit) {
					UUID uuid = UUID.randomUUID();
					bpmnlist.add(this.waveInitBpmnPart(x, workFlow, bean.getClass().getName(), uuid, method.getName()));
				}
			if (waveTransition.length != 0)
				for (WaveTransition x : waveTransition) {
					// ListenerCreator.createListener(bean, method, waveProperties.getPrefix() +
					// x.source_topic(),
					// waveProperties.getGroup_id());
					UUID uuid = UUID.randomUUID();
					bpmnlist.add(this.waveTransitionBpmnPart(x, workFlow, bean.getClass().getName(), uuid,
							method.getName()));
				}
			if (waveEnds.length != 0)
				for (WaveEnd x : waveEnds) { 
					UUID uuid = UUID.randomUUID();
					// ListenerCreator.createListener(bean, method, waveProperties.getPrefix() +
					// x.source_topic(),
					// waveProperties.getGroup_id());
					bpmnlist.add(this.waveEndBpmnPart(x, workFlow, bean.getClass().getName(), uuid, method.getName()));
				}
			if (ioEvents.length != 0) {
				for (IOEvent ioEvent : ioEvents) {
					if (ioEvent.startEvent().key().equals("")) {
						for (String topicName : ioEventService.getSourceTopic(ioEvent)) {

							if (!ListenerExist(topicName, bean, method,ioEvent)) {
								ListenerCreator.createListener(bean, method,ioEvent,waveProperties.getPrefix() + topicName, waveProperties.getGroup_id());
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
/**check if the listener already exist*/
	private boolean ListenerExist(String topicName, Object bean, Method method, IOEvent ioEvent) throws InterruptedException {
		for (Listener listener : listeners) {
			if (listener != null) {
				String t = listener.getTopic();
				if (t.equals(waveProperties.getPrefix()+topicName)) {
					listener.addBeanMethod(new BeanMethodPair(bean, method,ioEvent));
					return true;
				}
			}
		}
		return false;
	}

	/** methods to create IOEvent BPMN Parts from annotations **/
	private IOEventBpmnPart ioEventBpmnPart(IOEvent ioEvent, String className, UUID uuid, String methodName) {
		String processName="";
		if (!ioEvent.startEvent().key().equals(""))
		{processName=ioEvent.startEvent().key();}
		else if (!ioEvent.endEvent().key().equals("")) 
		{processName=ioEvent.endEvent().key();}
		IOEventBpmnPart ioeventBpmnPart = new IOEventBpmnPart(ioEvent, uuid, processName,ioEventService.getIOEventType(ioEvent), ioEvent.name(),
				className, methodName);
		return ioeventBpmnPart;
	}

	/** methods to create wave BPMN Parts from annotations **/
	private WaveBpmnPart waveInitBpmnPart(WaveInit x, String waveWorkFlowName, String className, UUID uuid,
			String methodName) {
		WaveBpmnPart waveinitBpmnPart = new WaveBpmnPart(uuid, "WaveInit", className, methodName, x.stepName(),
				waveWorkFlowName, x.target_event(), x.target_topic());
		return waveinitBpmnPart;
	}

	private WaveBpmnPart waveTransitionBpmnPart(WaveTransition x, String waveWorkFlowName, String className, UUID uuid,
			String methodName) {
		WaveBpmnPart wavetransitionBpmnPart = new WaveBpmnPart(uuid, "WaveTransition", className, methodName,
				x.stepName(), waveWorkFlowName, x.source_event(), x.target_event(), x.source_topic(), x.target_topic());
		return wavetransitionBpmnPart;
	}

	private WaveBpmnPart waveEndBpmnPart(WaveEnd x, String waveWorkFlowName, String className, UUID uuid,
			String methodName) {
		WaveBpmnPart waveEndBpmnPart = new WaveBpmnPart(uuid, "WaveEnd", className, methodName, x.stepName(),
				waveWorkFlowName, x.source_event(), x.source_topic());
		return waveEndBpmnPart;
	}

	/** get workFlow name from @WorkFlow annotation */
	private String getworkFlow(Object bean, WaveWorkFlow[] workFlow) {

		String workflowName = "";
		for (WaveWorkFlow waveWorkFlow : workFlow) {
			if (waveWorkFlow != null) {
				workflowName = waveWorkFlow.name();
				log.info(waveWorkFlow.name());
				log.info(bean.getClass().getName());

			}
		}
		return workflowName;
	}

	

}
