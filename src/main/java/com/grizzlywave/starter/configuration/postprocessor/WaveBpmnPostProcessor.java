package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.starter.GrizzlyWaveStarterApplication;
import com.grizzlywave.starter.annotations.WaveEnd;
import com.grizzlywave.starter.annotations.WaveInit;
import com.grizzlywave.starter.annotations.WaveTransition;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
import com.grizzlywave.starter.configuration.WaveConfigProperties;
import com.grizzlywave.starter.handler.ConsumerRecordsHandler;
import com.grizzlywave.starter.handler.FileWritingRecordsHandler;
import com.grizzlywave.starter.listener.Listener;

@Configuration
public class WaveBpmnPostProcessor implements BeanPostProcessor, WavePostProcessors {
	private static final Logger log = LoggerFactory.getLogger(GrizzlyWaveStarterApplication.class);

	@Autowired
	WaveConfigProperties waveProperties;
	
	@Autowired
	List<Map<String, Object>> bpmnlist;
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		try {

			this.process(bean,this.getworkFlow(bean, bean.getClass().getAnnotationsByType(WaveWorkFlow.class)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bean;
	}

	@Override
	public void process(Object bean,String workFlow) throws Exception {
		
		
		for (Method method : bean.getClass().getMethods()) {
			WaveInit[] waveInit = method.getAnnotationsByType(WaveInit.class);
			WaveTransition[] waveTransition = method.getAnnotationsByType(WaveTransition.class);
			WaveEnd[] waveEnds = method.getAnnotationsByType(WaveEnd.class);
			if (waveInit != null)
				for (WaveInit x : waveInit) {
					UUID uuid = UUID.randomUUID();

					bpmnlist.add(this.waveInitToMap(x,workFlow,bean.getClass().getName(),uuid,method.getName()));
				}

			if (waveTransition != null)
				for (WaveTransition x : waveTransition) {
					//this.createListener(waveProperties.getPrefix()+x.source_topic());
					UUID uuid = UUID.randomUUID();
					bpmnlist.add(this.waveTransitionToMap(x,workFlow,bean.getClass().getName(),uuid,method.getName()));


				}
			if (waveEnds != null)
				for (WaveEnd x : waveEnds) {
					UUID uuid = UUID.randomUUID();
					bpmnlist.add(this.waveEndToMap(x,workFlow,bean.getClass().getName(),uuid,method.getName()));


				}
		}
		}

	

	private Map<String, Object> waveInitToMap(WaveInit x, String waveWorkFlowName,String className, UUID uuid, String methodName) {
		Map<String, Object> mapResult = new LinkedHashMap<String, Object>();
		mapResult.put("id", uuid);
		mapResult.put("Type", "WaveInit");
		mapResult.put("workFlow", waveWorkFlowName);
		mapResult.put("ClassName", className);
		mapResult.put("MethodName", methodName);
		mapResult.put("stepName",x.stepName());
		mapResult.put("name", "Start");
		mapResult.put("target_event", x.target_event());
		mapResult.put("target_topic", x.target_topic());

		return mapResult;
	}

	private Map<String, Object> waveTransitionToMap(WaveTransition x,String waveWorkFlowName, String className, UUID uuid,String methodName) {
		Map<String, Object> mapResult = new LinkedHashMap<String, Object>();
		mapResult.put("id", uuid);
		mapResult.put("Type", "WaveTransition");
		mapResult.put("workFlow", waveWorkFlowName);
		mapResult.put("ClassName", className);
		mapResult.put("MethodName", methodName);
		mapResult.put("name", x.stepName());
		mapResult.put("source_event", x.source_event());
		mapResult.put("source_topic", x.source_topic());
		mapResult.put("target_event", x.target_event());
		mapResult.put("target_topic", x.target_topic());

		return mapResult;
	}

	private Map<String, Object> waveEndToMap(WaveEnd x,String waveWorkFlowName, String className, UUID uuid,String methodName) {
		Map<String, Object> mapResult = new LinkedHashMap<String, Object>();
		mapResult.put("id", uuid);
		mapResult.put("Type", "WaveEnd");
		mapResult.put("workFlow", waveWorkFlowName);
		mapResult.put("ClassName", className);
		mapResult.put("MethodName", methodName);
		mapResult.put("StepName", x.stepName());
		mapResult.put("source_event", x.source_event());
		mapResult.put("source_topic", x.source_topic());

		return mapResult;
	}
	private String getworkFlow(Object bean,WaveWorkFlow[] workFlow) {
		
		 String workflowName="";
		for (WaveWorkFlow waveWorkFlow : workFlow) {
			if (waveWorkFlow != null) {
				workflowName=waveWorkFlow.name();
				log.info(waveWorkFlow.name());
				log.info(bean.getClass().getName());

			}
			}
		return workflowName;
	}
	public void createListener(String topicName) {
		 Properties props = new Properties();
	        props.setProperty("bootstrap.servers", "192.168.99.100:9092");
	        props.setProperty("enable.auto.commit", "true");
	        props.setProperty("auto.commit.interval.ms", "1000");
	        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	        props.put("group.id", "consumer-group-1");
	        props.put("enable.auto.commit", "true");
	        props.put("auto.commit.interval.ms", "1000");
	        props.put("auto.offset.reset", "earliest");
	        props.put("session.timeout.ms", "30000");
	        props.put("topicName", topicName);
	        final Consumer<String, String> consumer = new KafkaConsumer<>(props);
	        final ConsumerRecordsHandler<String, String> recordsHandler = new FileWritingRecordsHandler();
	        final Listener consumerApplication = new Listener(consumer, recordsHandler);
	        Runtime.getRuntime().addShutdownHook(new Thread(consumerApplication::shutdown));

	        consumerApplication.runConsume(props);
	}
}
