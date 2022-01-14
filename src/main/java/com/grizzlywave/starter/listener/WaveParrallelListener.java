package com.grizzlywave.starter.listener;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.grizzlywave.starter.configuration.context.AppContext;
import com.grizzlywave.starter.configuration.postprocessor.BeanMethodPair;
import com.grizzlywave.starter.domain.IOEventHeaders;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;
import com.grizzlywave.starter.handler.RecordsHandler;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WaveParrallelListener {

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private List<Listener> listeners;
	@Autowired
	RecordsHandler recordsHandler;
	@Autowired
	private AppContext ctx;

	@Autowired
	private IOEventService ioEventService;

	@KafkaListener(topics = "resultTopic", containerFactory = "userKafkaListenerFactory", groupId = "${ioevent.group_id}")
	public void consumeParallelEvent(String s)
			throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		Gson gson = new Gson();
		WaveParallelEventInformation waveParallelEventInformation = gson.fromJson(s,
				WaveParallelEventInformation.class);
		if ((waveParallelEventInformation != null) && (sameList(waveParallelEventInformation.getSourceRequired(),
				waveParallelEventInformation.getTargetsArrived()))) {

			try {
				Object beanmObject = ctx.getApplicationContext()
						.getBean(Class.forName(waveParallelEventInformation.getClassName()));
				if (beanmObject != null) {
					StopWatch watch = new StopWatch();
					watch.start((String) waveParallelEventInformation.getHeaders()
							.get(IOEventHeaders.CORRELATION_ID.toString()));
					WaveRecordInfo waveRecordInfo = new WaveRecordInfo(
							waveParallelEventInformation.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString())
									.toString(),
							waveParallelEventInformation.getHeaders().get(IOEventHeaders.PROCESS_NAME.toString())
									.toString(),
							waveParallelEventInformation.getTargetsArrived().toString(), watch);
					WaveContextHolder.setContext(waveRecordInfo);

					invokeTargetMethod(waveParallelEventInformation.getMethod(), beanmObject,
							waveParallelEventInformation);

				}
			} catch (Throwable e) {
				log.error("error while invoking method ");
			}

		} else {
			log.info("Parallel Event Source Not Completed, target arrived : "
					+ waveParallelEventInformation.getTargetsArrived());
		}

	}

	/** method to invoke the method from a specific bean **/
	public void invokeTargetMethod(String methodName, Object beanmObject,
			WaveParallelEventInformation parallelEventInformation) throws Throwable {
		if (beanmObject != null) {

			for (Method met : beanmObject.getClass().getDeclaredMethods()) {
				if (met.getName().equals(methodName)) {
					Method method = met;
					
					if (met.getParameterCount() == 1) {
				
						recordsHandler.invokeWithOneParameter(method, beanmObject, parallelEventInformation.getValue());
					} else if (met.getParameterCount() == 2) {
						for (Listener listener : listeners) {
							Optional<BeanMethodPair> pair = listener.getBeanMethodPairs().stream()
									.filter(x -> (x.getBean().getClass().getName().equals(parallelEventInformation.getClassName())&&x.getMethod().getName().equals(parallelEventInformation.getMethod())))
									.findFirst();
							if (pair.isPresent()) {
								method=pair.get().getMethod();
							}
						}
						Object[] params = recordsHandler.prepareParameters(method, parallelEventInformation.getValue(),
								parallelEventInformation.getHeaders());
						recordsHandler.invokeWithtwoParameter(method, beanmObject, params);
					}

				}
			}

		}
	}

	public Object parseConsumedValue(Object consumedValue, Class<?> type) throws JsonProcessingException {
		if (type.equals(String.class)) {
			return consumedValue;
		} else {
			return mapper.readValue(consumedValue.toString(), type);
		}
	}

	public boolean sameList(List<String> firstList, List<String> secondList) {
		return (firstList.size() == secondList.size() && firstList.containsAll(secondList)
				&& secondList.containsAll(firstList));
	}
}
