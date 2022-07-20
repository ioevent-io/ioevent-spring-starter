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




package com.ioevent.starter.handler;






import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioevent.starter.annotations.IOHeaders;
import com.ioevent.starter.annotations.IOPayload;
import com.ioevent.starter.configuration.context.AppContext;
import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventParallelEventInformation;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/** Records handler to invoke method when consuming records from topic */
@Slf4j
@Service
public class RecordsHandler {

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private AppContext ctx;
	@Autowired
	private IOEventService ioEventService;

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	public Object parseConsumedValue(Object consumedValue, Class<?> type) throws JsonProcessingException {
		if (type.equals(String.class)) {
			return consumedValue;
		} else {
			return mapper.readValue(consumedValue.toString(), type);
		}
	}

	/**
	 * method to invoke the method from a specific bean
	 * 
	 * @throws JsonProcessingException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws BeansException
	 **/
	public void invokeWithOneParameter(Method method, Object bean, Object args) throws BeansException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonProcessingException {
		Class<?>[] params = method.getParameterTypes();
		method.invoke(ctx.getApplicationContext().getBean(bean.getClass()), parseConsumedValue(args, params[0]));

	}

	public void invokeWithtwoParameter(Method method, Object bean, Object[] params)
			throws BeansException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		method.invoke(ctx.getApplicationContext().getBean(bean.getClass()), params);
	}

	/**
	 * method called when the listener consume event , the method scan the header
	 * from consumer records and create ioeventRecordInfo from it , check if the
	 * output of the event equals to our methods Input , if our method annotation
	 * has parallel gateway :check if the list of Input are all arrived then send
	 * ioeventRecordInfo to aspect and call doinvoke(), else send ioeventRecordsInfo
	 * to aspect and call doinvoke()
	 **/

	public void process(ConsumerRecords<String, String> consumerRecords, List<BeanMethodPair> beanMethodPairs) {

		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {

			IOEventRecordInfo ioeventRecordInfo = this.getIOEventHeaders(consumerRecord);
			for (BeanMethodPair pair : beanMethodPairs) {

				for (String InputName : ioEventService.getInputNames(pair.getIoEvent())) {

					if (InputName.equals(ioeventRecordInfo.getOutputConsumedName())) {
						IOEventContextHolder.setContext(ioeventRecordInfo);
						if (pair.getIoEvent().gatewayInput().parallel()) {

							parallelInvoke(pair, consumerRecord, ioeventRecordInfo);

						} else {

							try {
								simpleInvokeMethod(pair, consumerRecord.value(), ioeventRecordInfo);
							} catch (BeansException | IllegalAccessException | IllegalArgumentException
									| InvocationTargetException | JsonProcessingException e) {
								log.error("error while invoking method");
							}
						}

					}
				}
			}

		}
	}

	public void parallelInvoke(BeanMethodPair pair, ConsumerRecord<String, String> consumerRecord,
			IOEventRecordInfo ioeventRecordInfo) {
		IOEventParallelEventInformation parallelEventInfo = new IOEventParallelEventInformation(consumerRecord,
				ioeventRecordInfo, pair, ioEventService.getInputNames(pair.getIoEvent()), appName);
		sendParallelInfo(parallelEventInfo);
		log.info("IOEventINFO : " + parallelEventInfo);
		log.info("parallel event arrived : " + ioeventRecordInfo.getOutputConsumedName());

	}

	public Message<IOEventParallelEventInformation> sendParallelInfo(
			IOEventParallelEventInformation parallelEventInfo) {

		Message<IOEventParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-parallel-gateway-events")
				.setHeader(KafkaHeaders.MESSAGE_KEY,
						parallelEventInfo.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString()).toString()
								+ parallelEventInfo.getInputRequired())
				.build();
		kafkaTemplate.send(message);
		kafkaTemplate.flush();
		return message;
	}

	private void simpleInvokeMethod(BeanMethodPair pair, String consumerValue, IOEventRecordInfo ioeventRecordInfo)
			throws BeansException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			JsonProcessingException {

		if (pair.getMethod().getParameterCount() == 1) {
			this.invokeWithOneParameter(pair.getMethod(), pair.getBean(), consumerValue);
		} else if (pair.getMethod().getParameterCount() == 2) {
			Map<String, Object> headersMap = ioeventRecordInfo.getHeaderList().stream()
					.collect(Collectors.toMap(Header::key, header -> new String(header.value())));
			Object[] params = prepareParameters(pair.getMethod(), consumerValue, headersMap);
			this.invokeWithtwoParameter(pair.getMethod(), pair.getBean(), params);
		} else {
			log.error("the method " + pair.getMethod().getName() + " must had one or two parameters");
		}
	}

	public Object[] prepareParameters(Method method, String consumerValue, Map<String, Object> headersMap)
			throws JsonProcessingException {
		Class[] parameterTypes = method.getParameterTypes();
		List<Object> paramList = new ArrayList<>();
		int payloadIndex = getIOPayloadIndex(method);

		int headerIndex = getIOHeadersIndex(method);
		if ((headerIndex >= 0) && (payloadIndex < 0)) {
			paramList.add(parseConsumedValue(consumerValue, parameterTypes[(headerIndex + 1) % 2]));
		} else {
			paramList.add(parseConsumedValue(consumerValue, parameterTypes[getIOPayloadIndex(method)]));
		}
		if (headerIndex >= 0) {
			paramList.add(headerIndex, headersMap);

		}
		return paramList.toArray();
	}

	public Object[] prepareParallelParameters(Method method, IOEventParallelEventInformation parallelEventConsumed)
			throws JsonProcessingException {
		Class[] parameterTypes = method.getParameterTypes();
		List<Object> paramList = new ArrayList<>();
		List<Integer> payloadIndex = getIOPayloadIndexlist(method);
		for (int i = 0; i < payloadIndex.size(); i++) {
			if (payloadIndex.get(i) >= 0) {
				String payloadInputName = parallelEventConsumed.getInputRequired().get(payloadIndex.get(i));
				paramList.add(parseConsumedValue(parallelEventConsumed.getPayloadMap().get(payloadInputName),
						parameterTypes[i]));
			} else if (payloadIndex.get(i) == -1) {
				paramList.add(parallelEventConsumed.getHeaders());
			} else {
				String payloadInputName = parallelEventConsumed.getInputRequired().get(0);
				paramList.add(parseConsumedValue(parallelEventConsumed.getPayloadMap().get(payloadInputName),
						parameterTypes[i]));
			}
		}

		return paramList.toArray();
	}

	private List<Integer> getIOPayloadIndexlist(Method method) {
		List<Integer> indexList = new ArrayList<>();
		Annotation[][] parametersAnnotations = method.getParameterAnnotations();
		for (Annotation[] parameterAnnotations : parametersAnnotations) {
			if (parameterAnnotations.length == 0) {
				indexList.add(-2);
			}
			for (Annotation annotations : parameterAnnotations) {
				try {
					IOPayload ioPayload = (IOPayload) annotations;
					indexList.add(ioPayload.index());
				} catch (Exception e) {
					try {
						IOHeaders ioHeaders = (IOHeaders) annotations;
						indexList.add(-1);
					} catch (Exception e2) {
						log.error("Bad Parameter Annotations use");
					}
				}

			}
		}
		return indexList;
	}

	private int getIOPayloadIndex(Method method) {
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		int parameterIndex = 0;
		for (Annotation[] annotations : parameterAnnotations) {
			if (Arrays.asList(annotations).stream().filter(IOPayload.class::isInstance).count() != 0) {
				return parameterIndex;
			}

			parameterIndex++;
		}
		return -1;
	}

	public int getIOHeadersIndex(Method method) {
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		int parameterIndex = 0;
		for (Annotation[] annotations : parameterAnnotations) {
			if (Arrays.asList(annotations).stream().filter(IOHeaders.class::isInstance).count() != 0) {
				return parameterIndex;
			}

			parameterIndex++;
		}
		return -1;
	}

	public List<String> parseStringToArray(String s) {
		List<String> output = new ArrayList<>();
		String listString = s.substring(1, s.length() - 1);
		String[] strings = listString.split(", ");
		for (String stringElement : strings) {
			output.add(stringElement.trim());
		}
		return output;
	}

	public IOEventRecordInfo getIOEventHeaders(ConsumerRecord<String, String> consumerRecord) {
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo();
		ioeventRecordInfo.setHeaderList(Arrays.asList(consumerRecord.headers().toArray()).stream()
				.filter(header -> !header.key().equals("spring_json_header_types")).collect(Collectors.toList()));
		StopWatch watch = new StopWatch();
		consumerRecord.headers().forEach(header -> {
			if (header.key().equals(IOEventHeaders.OUTPUT_EVENT.toString())) {
				ioeventRecordInfo.setOutputConsumedName(new String(header.value()));
			} else if (header.key().equals(IOEventHeaders.CORRELATION_ID.toString())) {
				ioeventRecordInfo.setId(new String(header.value()));
				watch.start(new String(header.value()));
			} else if (header.key().equals(IOEventHeaders.PROCESS_NAME.toString())) {
				ioeventRecordInfo.setWorkFlowName(new String(header.value()));
			} else if (header.key().equals(IOEventHeaders.START_INSTANCE_TIME.toString())) {
				ioeventRecordInfo.setInstanceStartTime(Long.valueOf(new String(header.value())));
			}

		});
		ioeventRecordInfo.setWatch(watch);
		return ioeventRecordInfo;
	}

}
