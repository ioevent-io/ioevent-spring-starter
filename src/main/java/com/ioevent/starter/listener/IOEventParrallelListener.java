/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	@KafkaListener(topics = "ioevent-parallel-gateway-aggregation", containerFactory = "userKafkaListenerFactory", groupId = "#{'${spring.kafka.consumer.group-id:${ioevent.group_id:${spring.application.name:ioevent_default_groupid}}}'}")
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
									.get(IOEventHeaders.START_INSTANCE_TIME.toString()).toString()),null);
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

					for (Listener listener : listeners) {
						Optional<BeanMethodPair> pair = listener.getBeanMethodPairs().stream().filter(
								x -> (x.getBean().getClass().getName().equals(parallelEventInformation.getClassName())
										&& x.getMethod().getName().equals(parallelEventInformation.getMethod())))
								.findFirst();
						if (pair.isPresent()) {
							method = pair.get().getMethod();
						}
					}
					Object[] params = recordsHandler.prepareParallelParameters(method, parallelEventInformation);
					recordsHandler.invokeWithtwoParameter(method, beanmObject, params);

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
