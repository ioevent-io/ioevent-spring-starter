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
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.listener.ListenerCreator;
import com.grizzlywave.starter.model.WaveBpmnPart;

import lombok.extern.slf4j.Slf4j;

/**
 *class configuration for Wave Bpmn Part Creation using Bean Post Processor 
 **/
@Slf4j
@Configuration
public class WaveBpmnPostProcessor implements BeanPostProcessor, WavePostProcessors{

	@Autowired
	private WaveProperties waveProperties;
	
	@Autowired
	private List<WaveBpmnPart> bpmnlist;

	@Autowired
	private ListenerCreator ListenerCreator;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)   {
		try {

			this.process(bean, this.getworkFlow(bean, bean.getClass().getAnnotationsByType(WaveWorkFlow.class)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)  {
		return bean;
	}
/**process method to check for annotations in the bean and create the Bpmn parts 
 **/
	@Override
	public void process(Object bean, String workFlow) throws Throwable  {

		for (Method method : bean.getClass().getMethods()) {
			WaveInit[] waveInit = method.getAnnotationsByType(WaveInit.class);
			WaveTransition[] waveTransition = method.getAnnotationsByType(WaveTransition.class);
			WaveEnd[] waveEnds = method.getAnnotationsByType(WaveEnd.class);
			if (waveInit.length!=0)
				for (WaveInit x : waveInit) {
					UUID uuid = UUID.randomUUID();
					bpmnlist.add(this.waveInitBpmnPart(x, workFlow, bean.getClass().getName(), uuid, method.getName()));
				}

			if (waveTransition.length!=0)
				for (WaveTransition x : waveTransition) {
					ListenerCreator.createListener(bean, method, waveProperties.getPrefix() + x.source_topic());
					UUID uuid = UUID.randomUUID();
					bpmnlist.add(this.waveTransitionBpmnPart(x, workFlow, bean.getClass().getName(), uuid, method.getName()));

				}
			if (waveEnds.length!=0)
				for (WaveEnd x : waveEnds) {
					UUID uuid = UUID.randomUUID();
					ListenerCreator.createListener(bean,method,waveProperties.getPrefix()+x.source_topic());
					bpmnlist.add(this.waveEndBpmnPart(x, workFlow, bean.getClass().getName(), uuid, method.getName()));

				}
		}
	}

/**methods to create wave BPMN Parts from annotations */
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
/**get workFlow name  from @WorkFlow annotation*/
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
