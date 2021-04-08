package com.grizzlywave.grizzlywavestarter.configuration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.grizzlywavestarter.GrizzlyWaveStarterApplication;
import com.grizzlywave.grizzlywavestarter.annotations.WaveEnd;
import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;

@Configuration
public class WaveBpmnPostProcessor implements BeanPostProcessor,WavePostProcessors {
	private static final Logger log = LoggerFactory.getLogger(GrizzlyWaveStarterApplication.class);

	public static Map<String, Object> bpmnPart = new HashMap<String, Object>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		try {
			this.process(bean);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bean;
	}

	@Override
	public void process(Object bean) throws Exception {
		for (Method method : bean.getClass().getMethods()) {
			WaveInit[] waveInit = method.getAnnotationsByType(WaveInit.class);
			WaveTransition[] waveTransition = method.getAnnotationsByType(WaveTransition.class);
			WaveEnd[] waveEnds = method.getAnnotationsByType(WaveEnd.class);
			if (waveInit != null)
				for (WaveInit x : waveInit) {
					UUID uuid = UUID.randomUUID();

					bpmnPart.put(uuid.toString(), this.waveInitToMap(x));
				}

			if (waveTransition != null)
				for (WaveTransition x : waveTransition) {
					UUID uuid = UUID.randomUUID();
					bpmnPart.put(uuid.toString(), this.waveTransitionToMap(x));

				}
			if (waveEnds != null)
				for (WaveEnd x : waveEnds) {
					UUID uuid = UUID.randomUUID();
					bpmnPart.put(uuid.toString(), this.waveEndToMap(x));

				}
		}

	}

	private Map<String, Object> waveInitToMap(WaveInit x) {
		Map<String, Object> mapResult = new HashMap<String, Object>();
		mapResult.put("Type", "WaveInit");
		mapResult.put("name","Start");
		mapResult.put("target_event", x.target_event());
		mapResult.put("target_topic", x.target_topic());

		return mapResult;
	}

	private Map<String, Object> waveTransitionToMap(WaveTransition x) {
		Map<String, Object> mapResult = new HashMap<String, Object>();
		mapResult.put("Type", "WaveTransition");
		mapResult.put("name", x.name());
		mapResult.put("source_event", x.source_event());
		mapResult.put("source_topic", x.source_topic());
		mapResult.put("target_event", x.target_event());
		mapResult.put("target_topic", x.target_topic());

		return mapResult;
	}

	private Map<String, Object> waveEndToMap(WaveEnd x) {
		Map<String, Object> mapResult = new HashMap<String, Object>();
		mapResult.put("Type", "WaveEnd");
		mapResult.put("name", x.name());
		mapResult.put("source_event", x.source_event());
		mapResult.put("source_topic", x.source_topic());

		return mapResult;
	}
}
	
