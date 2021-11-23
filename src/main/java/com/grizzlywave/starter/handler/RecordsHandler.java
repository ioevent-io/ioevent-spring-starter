package com.grizzlywave.starter.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.configuration.context.AppContext;
import com.grizzlywave.starter.configuration.postprocessor.BeanMethodPair;
import com.grizzlywave.starter.domain.ParallelEventInfo;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.WaveContextHolder;

import lombok.RequiredArgsConstructor;
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

	/** method which call doInvoke Method **/
	public void process(ConsumerRecords<String, String> consumerRecords, Object bean, Method method) throws Throwable {
		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
			this.InvokeWithOneParameter(method, bean, consumerRecord.value());
		}

	}

	public Object parseConsumedValue(Object consumedValue, Class<?> type)
			throws JsonMappingException, JsonProcessingException {
		if (type.equals(String.class)) {
			return consumedValue;
		} else {
			return mapper.readValue(consumedValue.toString(), type);
		}
	}

	/** method to invoke the method from a specific bean **/
	public void InvokeWithOneParameter(Method method, Object bean, Object args) throws Throwable {
		Object beanmObject = ctx.getApplicationContext().getBean(bean.getClass());
		if (beanmObject != null) {
			for (Method met : beanmObject.getClass().getDeclaredMethods()) {
				if (met.getName().equals(method.getName())) {
					Class<?>[] params = method.getParameterTypes();
					met.invoke(ctx.getApplicationContext().getBean(bean.getClass()),
							parseConsumedValue(args, params[0]));

				}
			}

		}
	}

	public void InvokeWithtwoParameter(Method method, Object bean, Object arg1, Object arg2) throws Throwable {
		Object beanmObject = ctx.getApplicationContext().getBean(bean.getClass());
		if (beanmObject != null) {
			for (Method met : beanmObject.getClass().getDeclaredMethods()) {
				if (met.getName().equals(method.getName())) {
					Class<?>[] params = method.getParameterTypes();
					met.invoke(ctx.getApplicationContext().getBean(bean.getClass()),
							parseConsumedValue(arg1, params[0]), arg2);

				}

			}
		}
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
			throws Throwable {

		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {

			WaveRecordInfo waveRecordInfo = this.getWaveHeaders(consumerRecord);
			for (BeanMethodPair pair : beanMethodPairs) {

				for (String SourceName : ioEventService.getSourceNames(pair.getIoEvent())) {

					if (SourceName.equals(waveRecordInfo.getTargetName())) {
						WaveContextHolder.setContext(waveRecordInfo);
						if (pair.getIoEvent().gatewaySource().parallel()) {

							parallelInvoke(pair, consumerRecord, waveRecordInfo);

						} else {

							simpleInvoke(pair, consumerRecord, waveRecordInfo);
						}

					}
				}
			}

		}
	}

	private void parallelInvoke(BeanMethodPair pair, ConsumerRecord<String, String> consumerRecord,
			WaveRecordInfo waveRecordInfo) throws Throwable {
		WaveParallelEventInformation parallelEventInfo = new WaveParallelEventInformation(consumerRecord,
				waveRecordInfo, pair,ioEventService.getSourceNames(pair.getIoEvent()),appName);
		sendParallelInfo(parallelEventInfo);
		log.info("IOEventINFO : " + parallelEventInfo);
		log.info("parallel event arrived : " + waveRecordInfo.getTargetName());
		

	}

	private void sendParallelInfo(WaveParallelEventInformation parallelEventInfo) throws InterruptedException {
		
		  Message<WaveParallelEventInformation> message =
		  MessageBuilder.withPayload(parallelEventInfo) .setHeader(KafkaHeaders.TOPIC,
		  "ParallelEventTopic") .setHeader(KafkaHeaders.MESSAGE_KEY,
		  parallelEventInfo.getHeaders().get("Correlation_id")+parallelEventInfo.getSourceRequired()).build(); 
		  kafkaTemplate.send(message); 
		  kafkaTemplate.flush();
		 
	}

	private void simpleInvoke(BeanMethodPair pair, ConsumerRecord<String, String> consumerRecord,
			WaveRecordInfo waveRecordInfo) throws Throwable {
		this.invokeMethod(pair, consumerRecord.value(), waveRecordInfo);
	}

	private void invokeMethod(BeanMethodPair pair, String consumerValue, WaveRecordInfo waveRecordInfo)
			throws Throwable {

		if (pair.getMethod().getParameterCount() == 1) {
			this.InvokeWithOneParameter(pair.getMethod(), pair.getBean(), consumerValue);
		} else if (pair.getMethod().getParameterCount() == 2) {
			this.InvokeWithtwoParameter(pair.getMethod(), pair.getBean(), consumerValue,
					waveRecordInfo.getTargetName());
		} else {
			log.error("the method " + pair.getMethod().getName() + " must had one or two parameters");
		}
	}

	
	public List<String> parseStringToArray(String s) {
		List<String> output = new ArrayList<String>();
		String listString = s.substring(1, s.length() - 1);
		String[] strings = listString.split(", ");
		for (String stringElement : strings) {
			output.add(stringElement.trim());
		}
		return output;
	}

	public WaveRecordInfo getWaveHeaders(ConsumerRecord<String, String> consumerRecord) {
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo();
		waveRecordInfo.setHeaderList(Arrays.asList(consumerRecord.headers().toArray()));
		StopWatch watch=new StopWatch();
		consumerRecord.headers().forEach(header -> {
			if (header.key().equals("targetEvent")) {
				waveRecordInfo.setTargetName(new String(header.value()));
			}else if (header.key().equals("Correlation_id")) {
				waveRecordInfo.setId(new String(header.value()));
				watch.start(new String(header.value()));
			}else if (header.key().equals("Process_Name")) {
				waveRecordInfo.setWorkFlowName(new String(header.value()));
			}
			
		});
		waveRecordInfo.setWatch(watch);
		return waveRecordInfo;
	}

}
