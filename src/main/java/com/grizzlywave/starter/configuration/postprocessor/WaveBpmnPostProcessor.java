package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import com.grizzlywave.starter.annotations.WaveEnd;
import com.grizzlywave.starter.annotations.WaveInit;
import com.grizzlywave.starter.annotations.WaveTransition;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
import com.grizzlywave.starter.listener.ListenerCreator;
import com.grizzlywave.starter.model.WaveBpmnPart;

@Configuration
public class WaveBpmnPostProcessor implements BeanPostProcessor, WavePostProcessors, BeanFactoryAware {
	private static final Logger log = LoggerFactory.getLogger(WaveBpmnPostProcessor.class);

	// @Autowired
	// private WaveProperties waveProperties;
	private BeanFactory beanFactory;

	@Autowired
	private List<WaveBpmnPart> bpmnlist;

	@Autowired
	private ListenerCreator ListenerCreator;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
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
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		/*
		 * for (Method method : bean.getClass().getMethods()) { WaveInit[] waveInit =
		 * method.getAnnotationsByType(WaveInit.class); WaveTransition[] waveTransition
		 * = method.getAnnotationsByType(WaveTransition.class); WaveEnd[] waveEnds =
		 * method.getAnnotationsByType(WaveEnd.class);
		 * log.info("the anno"+waveEnds.length); log.info("the anno"+waveInit.length);
		 * log.info("the anno"+waveTransition.length); if (waveInit != null) for
		 * (WaveInit x : waveInit) { log.info("the anno"+x.stepName()); }
		 * 
		 * if (waveTransition != null) for (WaveTransition x : waveTransition) {
		 * log.info("the anno"+x.stepName());
		 * 
		 * 
		 * } if (waveEnds != null) for (WaveEnd x : waveEnds) {
		 * log.info("the anno"+x.stepName());
		 * 
		 * 
		 * } }
		 */
		return bean;
	}

	@Override
	public void process(Object bean, String workFlow) throws Throwable {

		for (Method method : bean.getClass().getMethods()) {
			WaveInit[] waveInit = method.getAnnotationsByType(WaveInit.class);
			WaveTransition[] waveTransition = method.getAnnotationsByType(WaveTransition.class);
			WaveEnd[] waveEnds = method.getAnnotationsByType(WaveEnd.class);
			if (waveInit != null)
				for (WaveInit x : waveInit) {
					UUID uuid = UUID.randomUUID();

					bpmnlist.add(this.waveInitToMap(x, workFlow, bean.getClass().getName(), uuid, method.getName()));
				}

			if (waveTransition != null)
				for (WaveTransition x : waveTransition) {
					Method methodToUse = checkProxy(method, bean);
					ListenerCreator.createListener(bean, methodToUse, "CodeOnce-" + x.source_topic());
					UUID uuid = UUID.randomUUID();
					bpmnlist.add(
							this.waveTransitionToMap(x, workFlow, bean.getClass().getName(), uuid, method.getName()));

				}
			if (waveEnds != null)
				for (WaveEnd x : waveEnds) {
					UUID uuid = UUID.randomUUID();
					// ListenerCreator.createListener(bean,method,waveProperties.getPrefix()+x.source_topic());
					bpmnlist.add(this.waveEndToMap(x, workFlow, bean.getClass().getName(), uuid, method.getName()));

				}
		}
	}

	private Method checkProxy(Method methodArg, Object bean) {
		Method method = methodArg;
		if (AopUtils.isJdkDynamicProxy(bean)) {
			try {
				// Found a @KafkaListener method on the target class for this JDK proxy ->
				// is it also present on the proxy itself?
				method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
				Class<?>[] proxiedInterfaces = ((Advised) bean).getProxiedInterfaces();
				for (Class<?> iface : proxiedInterfaces) {
					try {
						method = iface.getMethod(method.getName(), method.getParameterTypes());
						break;
					} catch (@SuppressWarnings("unused") NoSuchMethodException noMethod) {
						// NOSONAR
					}
				}
			} catch (SecurityException ex) {
				ReflectionUtils.handleReflectionException(ex);
			} catch (NoSuchMethodException ex) {
				throw new IllegalStateException(String.format(
						"@KafkaListener method '%s' found on bean target class '%s', "
								+ "but not found in any interface(s) for bean JDK proxy. Either "
								+ "pull the method up to an interface or switch to subclass (CGLIB) "
								+ "proxies by setting proxy-target-class/proxyTargetClass " + "attribute to 'true'",
						method.getName(), method.getDeclaringClass().getSimpleName()), ex);
			}
		}
		return method;
	}

	private WaveBpmnPart waveInitToMap(WaveInit x, String waveWorkFlowName, String className, UUID uuid,
			String methodName) {
		WaveBpmnPart waveinitBpmnPart = new WaveBpmnPart(uuid, "WaveInit", className, methodName, x.stepName(),
				waveWorkFlowName, x.target_event(), x.target_topic());
		return waveinitBpmnPart;
	}

	
	private WaveBpmnPart waveTransitionToMap(WaveTransition x, String waveWorkFlowName, String className, UUID uuid,
			String methodName) {
		WaveBpmnPart wavetransitionBpmnPart = new WaveBpmnPart(uuid, "WaveTransition", className, methodName,
				x.stepName(), waveWorkFlowName, x.source_event(), x.target_event(), x.source_topic(), x.target_topic());
		return wavetransitionBpmnPart;
	}

	private WaveBpmnPart waveEndToMap(WaveEnd x, String waveWorkFlowName, String className, UUID uuid,
			String methodName) {
		WaveBpmnPart waveEndBpmnPart = new WaveBpmnPart(uuid, "WaveEnd", className, methodName, x.stepName(),
				waveWorkFlowName, x.source_event(), x.source_topic());
		return waveEndBpmnPart;
	}

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

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/*
	 * public void createListener(String topicName) { Properties props = new
	 * Properties(); props.setProperty("bootstrap.servers", "192.168.99.100:9092");
	 * props.setProperty("enable.auto.commit", "true");
	 * props.setProperty("auto.commit.interval.ms", "1000");
	 * props.setProperty("key.deserializer",
	 * "org.apache.kafka.common.serialization.StringDeserializer");
	 * props.setProperty("value.deserializer",
	 * "org.apache.kafka.common.serialization.StringDeserializer");
	 * props.put("group.id", "consumer-group-1"); props.put("enable.auto.commit",
	 * "true"); props.put("auto.commit.interval.ms", "1000");
	 * props.put("auto.offset.reset", "earliest"); props.put("session.timeout.ms",
	 * "30000"); props.put("topicName", topicName); final Consumer<String, String>
	 * consumer = new KafkaConsumer<>(props); final ConsumerRecordsHandler<String,
	 * String> recordsHandler = new FileWritingRecordsHandler(); final Listener
	 * consumerApplication = new Listener(consumer, recordsHandler);
	 * Runtime.getRuntime().addShutdownHook(new
	 * Thread(consumerApplication::shutdown));
	 * 
	 * consumerApplication.runConsume(props); }
	 */
}
