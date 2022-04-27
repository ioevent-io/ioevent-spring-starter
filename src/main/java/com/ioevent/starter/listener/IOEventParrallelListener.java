package com.ioevent.starter.listener;

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
import com.ioevent.starter.configuration.context.AppContext;
import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventParallelEventInformation;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.handler.RecordsHandler;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IOEventParrallelListener {

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private List<Listener> listeners;
	@Autowired
	RecordsHandler recordsHandler;
	@Autowired
	private AppContext ctx;

	@Autowired
	private IOEventService ioEventService;

	@KafkaListener(topics = "resultTopic", containerFactory = "userKafkaListenerFactory", groupId = "#{'${spring.kafka.consumer.group-id:${ioevent.group_id:ioevent}}'}")
	public void consumeParallelEvent(String s)
			throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		Gson gson = new Gson();
		IOEventParallelEventInformation ioeventParallelEventInformation = gson.fromJson(s,
				IOEventParallelEventInformation.class);
		if ((ioeventParallelEventInformation != null) && (sameList(ioeventParallelEventInformation.getInputRequired(),
				ioeventParallelEventInformation.getInputsArrived()))) {

			try {
				Object beanmObject = ctx.getApplicationContext()
						.getBean(Class.forName(ioeventParallelEventInformation.getClassName()));
				if (beanmObject != null) {
					StopWatch watch = new StopWatch();
					watch.start((String) ioeventParallelEventInformation.getHeaders()
							.get(IOEventHeaders.CORRELATION_ID.toString()));
					IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo(
							ioeventParallelEventInformation.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString())
									.toString(),
							ioeventParallelEventInformation.getHeaders().get(IOEventHeaders.PROCESS_NAME.toString())
									.toString(),
							ioeventParallelEventInformation.getInputsArrived().toString(), watch,
							Long.valueOf(ioeventParallelEventInformation.getHeaders()
									.get(IOEventHeaders.START_INSTANCE_TIME.toString()).toString()));
					IOEventContextHolder.setContext(ioeventRecordInfo);

					invokeTargetMethod(ioeventParallelEventInformation.getMethod(), beanmObject,
							ioeventParallelEventInformation);

				}
			} catch (Throwable e) {
				log.error("error while invoking method ");
			}

		} else {
			log.info("Parallel Event Input Not Completed, output arrived : "
					+ ioeventParallelEventInformation.getInputsArrived());
		}

	}

	/** method to invoke the method from a specific bean **/
	public void invokeTargetMethod(String methodName, Object beanmObject,
			IOEventParallelEventInformation parallelEventInformation) throws Throwable {
		if (beanmObject != null) {

			for (Method met : beanmObject.getClass().getDeclaredMethods()) {
				if (met.getName().equals(methodName)) {
					Method method = met;

					if (met.getParameterCount() == 1) {
						for (Listener listener : listeners) {
							Optional<BeanMethodPair> pair = listener.getBeanMethodPairs().stream()
									.filter(x -> (x.getBean().getClass().getName()
											.equals(parallelEventInformation.getClassName())
											&& x.getMethod().getName().equals(parallelEventInformation.getMethod())))
									.findFirst();
							if (pair.isPresent()) {
								method = pair.get().getMethod();
							}
						}
						// recordsHandler.invokeWithOneParameter(method, beanmObject,
						// parallelEventInformation.getValue());
						Object[] params = recordsHandler.prepareParallelParameters(method, parallelEventInformation);
						recordsHandler.invokeWithtwoParameter(method, beanmObject, params);
					} else {
						for (Listener listener : listeners) {
							Optional<BeanMethodPair> pair = listener.getBeanMethodPairs().stream()
									.filter(x -> (x.getBean().getClass().getName()
											.equals(parallelEventInformation.getClassName())
											&& x.getMethod().getName().equals(parallelEventInformation.getMethod())))
									.findFirst();
							if (pair.isPresent()) {
								method = pair.get().getMethod();
							}
						}
						Object[] params = recordsHandler.prepareParallelParameters(method, parallelEventInformation);
						// recordsHandler.prepareParameters(method,
						// parallelEventInformation.getValue(),parallelEventInformation.getHeaders());
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
