package com.grizzlywave.starter.handler;

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
import com.grizzlywave.starter.annotations.v2.IOHeaders;
import com.grizzlywave.starter.annotations.v2.IOPayload;
import com.grizzlywave.starter.configuration.context.AppContext;
import com.grizzlywave.starter.configuration.postprocessor.BeanMethodPair;
import com.grizzlywave.starter.domain.IOEventHeaders;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

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

	public Object parseConsumedValue(Object consumedValue, Class<?> type)
			throws  JsonProcessingException {
		if (type.equals(String.class)) {
			return consumedValue;
		} else {
			return mapper.readValue(consumedValue.toString(), type);
		}
	}

	/** method to invoke the method from a specific bean 
	 * @throws JsonProcessingException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws  
	 * @throws BeansException **/
	public void invokeWithOneParameter(Method method, Object bean, Object args) throws BeansException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonProcessingException   {
		Class<?>[] params = method.getParameterTypes();
		method.invoke(ctx.getApplicationContext().getBean(bean.getClass()), parseConsumedValue(args, params[0]));

	}

	public void invokeWithtwoParameter(Method method, Object bean, Object[] params)
			throws BeansException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		method.invoke(ctx.getApplicationContext().getBean(bean.getClass()), params);
	}

	/**
	 * method called when the listener consume event , the method scan the header
	 * from consumer records and create waveRecordInfo from it , check if the target
	 * of the event equals to our methods source , if our method annotation has
	 * parallel gateway :check if the list of source are all arrived then send
	 * waveRecordInfo to aspect and call doinvoke(), else send waveRecordsInfo to
	 * aspect and call doinvoke()
	 **/

	public void process(ConsumerRecords<String, String> consumerRecords, List<BeanMethodPair> beanMethodPairs)
			 {

		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {

			WaveRecordInfo waveRecordInfo = this.getWaveHeaders(consumerRecord);
			for (BeanMethodPair pair : beanMethodPairs) {

				for (String SourceName : ioEventService.getSourceNames(pair.getIoEvent())) {

					if (SourceName.equals(waveRecordInfo.getTargetName())) {
						WaveContextHolder.setContext(waveRecordInfo);
						if (pair.getIoEvent().gatewaySource().parallel()) {

							parallelInvoke(pair, consumerRecord, waveRecordInfo);

						} else {

							try {
								simpleInvokeMethod(pair, consumerRecord.value(), waveRecordInfo);
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
			WaveRecordInfo waveRecordInfo) {
		WaveParallelEventInformation parallelEventInfo = new WaveParallelEventInformation(consumerRecord,
				waveRecordInfo, pair, ioEventService.getSourceNames(pair.getIoEvent()), appName);
		sendParallelInfo(parallelEventInfo);
		log.info("IOEventINFO : " + parallelEventInfo);
		log.info("parallel event arrived : " + waveRecordInfo.getTargetName());

	}

	public Message<WaveParallelEventInformation> sendParallelInfo(WaveParallelEventInformation parallelEventInfo) {

		Message<WaveParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ParallelEventTopic")
				.setHeader(KafkaHeaders.MESSAGE_KEY,
						parallelEventInfo.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString()).toString()
								+ parallelEventInfo.getSourceRequired())
				.build();
		kafkaTemplate.send(message);
		kafkaTemplate.flush();
		return message;
	}



	private void simpleInvokeMethod(BeanMethodPair pair, String consumerValue, WaveRecordInfo waveRecordInfo) throws BeansException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonProcessingException
			 {

		if (pair.getMethod().getParameterCount() == 1) {
			this.invokeWithOneParameter(pair.getMethod(), pair.getBean(), consumerValue);
		} else if (pair.getMethod().getParameterCount() == 2) {
			Map<String, Object> headersMap = waveRecordInfo.getHeaderList().stream()
					.collect(Collectors.toMap(Header::key, header -> new String(header.value())));
			Object[] params = prepareParameters(pair.getMethod(), consumerValue, headersMap);
			this.invokeWithtwoParameter(pair.getMethod(), pair.getBean(), params);
		} else {
			log.error("the method " + pair.getMethod().getName() + " must had one or two parameters");
		}
	}

	public Object[] prepareParameters(Method method, String consumerValue, Map<String, Object> headersMap)
			throws  JsonProcessingException {
		Class[] parameterTypes = method.getParameterTypes();
		List<Object> paramList = new ArrayList<>();
		int payloadIndex = getIOPayloadIndex(method);

		int headerIndex = getIOHeadersIndex(method);
		if ((headerIndex >= 0)&&(payloadIndex < 0)) {
			paramList.add(parseConsumedValue(consumerValue, parameterTypes[(headerIndex + 1) % 2]));
		} else {
			paramList.add(parseConsumedValue(consumerValue, parameterTypes[getIOPayloadIndex(method)]));
		}
		if (headerIndex >= 0) {
			paramList.add(headerIndex, headersMap);

		}
		return paramList.toArray();
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

	public WaveRecordInfo getWaveHeaders(ConsumerRecord<String, String> consumerRecord) {
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo();
		waveRecordInfo.setHeaderList(Arrays.asList(consumerRecord.headers().toArray()).stream()
				.filter(header -> !header.key().equals("spring_json_header_types")).collect(Collectors.toList()));
		StopWatch watch = new StopWatch();
		consumerRecord.headers().forEach(header -> {
			if (header.key().equals(IOEventHeaders.TARGET_EVENT.toString())) {
				waveRecordInfo.setTargetName(new String(header.value()));
			} else if (header.key().equals(IOEventHeaders.CORRELATION_ID.toString())) {
				waveRecordInfo.setId(new String(header.value()));
				watch.start(new String(header.value()));
			} else if (header.key().equals(IOEventHeaders.PROCESS_NAME.toString())) {
				waveRecordInfo.setWorkFlowName(new String(header.value()));
			}

		});
		waveRecordInfo.setWatch(watch);
		return waveRecordInfo;
	}

}
