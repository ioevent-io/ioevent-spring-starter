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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ioevent.starter.annotations.IOHeader;
import com.ioevent.starter.annotations.IOHeaders;
import com.ioevent.starter.annotations.IOPayload;
import com.ioevent.starter.configuration.context.AppContext;
import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventMessageEventInformation;
import com.ioevent.starter.domain.IOEventParallelEventInformation;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

/** Records handler to invoke method when consuming records from topic */
@Slf4j
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

	@Autowired
	private ScheduledExecutorService asyncExecutor;


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
			throws IllegalAccessException, InvocationTargetException {
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

			String outputConsumed = this.getIOEventHeaders(consumerRecord).getOutputConsumedName();
			for (BeanMethodPair pair : beanMethodPairs) {
				String messgeKeyExpected = pair.getIoEvent().message().key();

				for (String InputName : ioEventService.getInputNames(pair.getIoEvent())) {
					TimeUnit timeUnit = (pair.getIoEvent().timer().timeUnit() != null)
							? pair.getIoEvent().timer().timeUnit()
							: TimeUnit.SECONDS;
					long duration = (pair.getIoEvent().timer().delay() > 0) ? pair.getIoEvent().timer().delay() : 0L;

					if (InputName.equals(outputConsumed)
					) {
						asyncExecutor.schedule(() -> {
							IOEventRecordInfo ioeventRecordInfo = this.getIOEventHeaders(consumerRecord);
							IOEventContextHolder.setContext(ioeventRecordInfo);
							if (pair.getIoEvent().gatewayInput().parallel()) {
								parallelInvoke(pair, consumerRecord, ioeventRecordInfo);

							} else if (ioEventService.isMessage(pair.getIoEvent())
									&& ioEventService.isMessageCatch(pair.getIoEvent())) {
								messageInvoke(pair, consumerRecord, ioeventRecordInfo);
							} else {
								try {
									simpleInvokeMethod(pair, consumerRecord.value(), ioeventRecordInfo);
								} catch (IllegalAccessException | InvocationTargetException
										| JsonProcessingException e) {
									log.error("error while invoking method", e);
								}
							}
						}, duration, timeUnit);
					} else if (isMessageThrowEvent(consumerRecord)) {
						asyncExecutor.schedule(() -> {
							IOEventRecordInfo ioeventRecordInfo = this.getIOEventHeaders(consumerRecord);
							if (ioEventService.isMessage(pair.getIoEvent())
									&& ioEventService.isMessageCatch(pair.getIoEvent())) {
								messageInvoke(pair, consumerRecord, ioeventRecordInfo);
							}
						}, duration, timeUnit);
					}

				}

			}

		}
	}

	private boolean isMessageThrowEvent(ConsumerRecord<String, String> consumerRecord) {
		return this.getIOEventHeaders(consumerRecord).getTaskType().equals(IOEventType.MESSAGE_THROW.toString());
	}

	public void parallelInvoke(BeanMethodPair pair, ConsumerRecord<String, String> consumerRecord,
			IOEventRecordInfo ioeventRecordInfo) {
		IOEventParallelEventInformation parallelEventInfo = new IOEventParallelEventInformation(consumerRecord,
				ioeventRecordInfo, pair, ioEventService.getInputNames(pair.getIoEvent()), appName);
		sendParallelInfo(parallelEventInfo);
		log.info("IOEventINFO : " + parallelEventInfo);
		log.info("parallel event arrived : " + ioeventRecordInfo.getOutputConsumedName());

	}

	public void messageInvoke(BeanMethodPair pair, ConsumerRecord<String, String> consumerRecord,
			IOEventRecordInfo ioeventRecordInfo) {

		IOEventMessageEventInformation messageEventInfo = new IOEventMessageEventInformation(consumerRecord,
				ioeventRecordInfo, pair, ioEventService.getInputNames(pair.getIoEvent()), appName,
				pair.getIoEvent().message().key());
		sendMessageInfo(messageEventInfo);
		log.info("IOEventINFO : " + messageEventInfo);
		log.info("message event arrived : " + ioeventRecordInfo.getOutputConsumedName());

	}

	public Message<IOEventParallelEventInformation> sendParallelInfo(
			IOEventParallelEventInformation parallelEventInfo) {

		Message<IOEventParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-parallel-gateway-events")
				.setHeader(KafkaHeaders.KEY,
						parallelEventInfo.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString()).toString()
								+ parallelEventInfo.getInputRequired())
				.build();
		kafkaTemplate.send(message);
		kafkaTemplate.flush();
		return message;
	}

	public Message<IOEventMessageEventInformation> sendMessageInfo(IOEventMessageEventInformation messageEventInfo) {

		Message<IOEventMessageEventInformation> message = MessageBuilder.withPayload(messageEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ioevent-message-events")
				.setHeader(KafkaHeaders.MESSAGE_KEY,
						messageEventInfo.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString()).toString()
								+ messageEventInfo.getInputRequired() + messageEventInfo.getMessageEventRequired())
				.build();
		kafkaTemplate.send(message);
		kafkaTemplate.flush();
		return message;
	}

	private void simpleInvokeMethod(BeanMethodPair pair, String consumerValue, IOEventRecordInfo ioeventRecordInfo)
			throws IllegalAccessException, InvocationTargetException, JsonProcessingException {

		Map<String, Object> headersMap = ioeventRecordInfo.getHeaderList().stream()
				.collect(Collectors.toMap(Header::key, header -> new String(header.value())));
		Object[] params = prepareParameters(pair.getMethod(), consumerValue, headersMap);
		this.invokeWithtwoParameter(pair.getMethod(), pair.getBean(), params);

	}

	public Object[] prepareParameters(Method method, String consumerValue, Map<String, Object> headersMap)
			throws JsonProcessingException {
		Class[] parameterTypes = method.getParameterTypes();
		List<Object> paramList = new ArrayList<>();

		List<Integer> headerIndexlist = getIOHeaderIndexList(method);
		Map<Integer, Object> param = getParamMap(method, consumerValue, headersMap);

		for (int i = 0; i < parameterTypes.length; i++) {
			if (param.get(i) == null) {
				paramList.add(parseConsumedValue(consumerValue, parameterTypes[i]));
			} else if (param.get(i).equals("no such header exist")) {
				paramList.add(null);

			} else {
				paramList.add(param.get(i));
			}
		}

		return paramList.toArray();
	}

	public Object[] prepareParallelParameters(Method method, IOEventParallelEventInformation parallelEventConsumed)
			throws JsonProcessingException {
		Class[] parameterTypes = method.getParameterTypes();
		List<Object> paramList = new ArrayList<>();
		Map<Integer, Object> param = getParallelParamMap(method, parallelEventConsumed);
		for (int i = 0; i < parameterTypes.length; i++) {
			if (param.get(i) == null) {
				String payloadInputName = parallelEventConsumed.getInputRequired().get(0);
				paramList.add(parseConsumedValue(parallelEventConsumed.getPayloadMap().get(payloadInputName),
						parameterTypes[i]));
			} else if (param.get(i).equals("no such header exist")) {
				paramList.add(null);

			} else {
				paramList.add(param.get(i));
			}
		}
		return paramList.toArray();
	}

	private Map<Integer, Object> getParallelParamMap(Method method,
			IOEventParallelEventInformation parallelEventConsumed) throws JsonProcessingException {
		Class[] parameterTypes = method.getParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		Map<Integer, Object> paramMap = new HashMap<>();
		for (int i = 0; i < parameterAnnotations.length; i++) {
			Annotation[] annotations = parameterAnnotations[i];
			if (Arrays.asList(annotations).stream().filter(IOPayload.class::isInstance).count() != 0) {
				for (Annotation annotation : annotations) {
					IOPayload ioPayload = (IOPayload) annotation;
					String payloadInputName = parallelEventConsumed.getInputRequired().get(ioPayload.index());
					paramMap.put(i, parseConsumedValue(parallelEventConsumed.getPayloadMap().get(payloadInputName),
							parameterTypes[i]));
				}
			}
			if (Arrays.asList(annotations).stream().filter(IOHeader.class::isInstance).count() != 0) {
				for (Annotation annotation : annotations) {
					IOHeader ioHeader = (IOHeader) annotation;
					paramMap.put(i,
							parallelEventConsumed.getHeaders().get(ioHeader.value()) != null ? parseConsumedValue(
									parallelEventConsumed.getHeaders().get(ioHeader.value()), parameterTypes[i])
									: "no such header exist");
				}

			}
			if (Arrays.asList(annotations).stream().filter(IOHeaders.class::isInstance).count() != 0) {
				paramMap.put(i, parallelEventConsumed.getHeaders());
			}
		}
		return paramMap;
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

	public List<Integer> getIOHeaderIndexList(Method method) {
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		List<Integer> parameterIndexList = new ArrayList<>();
		for (int i = 0; i < parameterAnnotations.length; i++) {
			Annotation[] annotations = parameterAnnotations[i];
			if (Arrays.asList(annotations).stream().filter(IOHeader.class::isInstance).count() != 0) {
				parameterIndexList.add(i);
			}
		}
		return parameterIndexList;
	}

	public Map<Integer, Object> getParamMap(Method method, String consumerValue, Map<String, Object> headersMap)
			throws JsonProcessingException {
		Class[] parameterTypes = method.getParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		Map<Integer, Object> paramMap = new HashMap<>();
		for (int i = 0; i < parameterAnnotations.length; i++) {
			Annotation[] annotations = parameterAnnotations[i];
			if (Arrays.asList(annotations).stream().filter(IOPayload.class::isInstance).count() != 0) {
				paramMap.put(i, parseConsumedValue(consumerValue, parameterTypes[i]));
			}
			if (Arrays.asList(annotations).stream().filter(IOHeader.class::isInstance).count() != 0) {
				for (Annotation annotation : annotations) {
					IOHeader ioHeader = (IOHeader) annotation;
					paramMap.put(i,
							headersMap.get(ioHeader.value()) != null
									? parseConsumedValue(headersMap.get(ioHeader.value()), parameterTypes[i])
									: "no such header exist");
				}

			}
			if (Arrays.asList(annotations).stream().filter(IOHeaders.class::isInstance).count() != 0) {
				paramMap.put(i, headersMap);
			}
		}
		return paramMap;

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
				.filter(header -> !header.key().equals("spring_json_header_types"))
				.filter(header -> !header.key().equals(IOEventHeaders.ERROR_TYPE.toString())
						&& !header.key().equals(IOEventHeaders.ERROR_MESSAGE.toString())
						&& !header.key().equals(IOEventHeaders.ERROR_TRACE.toString()))
				.filter(header -> !header.key().equals(IOEventHeaders.RESUME.toString())).collect(Collectors.toList()));
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
			} else if (header.key().equals(IOEventHeaders.MESSAGE_KEY.toString())) {
				ioeventRecordInfo.setMessageKey(new String(header.value()));
			} else if (header.key().equals(IOEventHeaders.EVENT_TYPE.toString())) {
				ioeventRecordInfo.setTaskType(new String(header.value()));
			}

		});
		ioeventRecordInfo.setWatch(watch);
		return ioeventRecordInfo;
	}

}
