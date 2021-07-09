package com.grizzlywave.starter.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.configuration.context.AppContext;
import com.grizzlywave.starter.configuration.postprocessor.BeanMethodPair;
import com.grizzlywave.starter.domain.ParallelEventInfo;
import com.grizzlywave.starter.service.IOEventService;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.Row;
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

	@Autowired
	private Client KsqlClient;
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	/** method which call doInvoke Method **/
	public void process(ConsumerRecords<String, String> consumerRecords, Object bean, Method method) throws Throwable {
		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
			this.doInvoke(method, bean, consumerRecord.value());
		}

	}

	/** method to invoke the method from a specific bean **/
	public void doInvoke(Method method, Object bean, Object args) throws Throwable {
		Object beanmObject = ctx.getApplicationContext().getBean(bean.getClass());
		if (beanmObject != null) {
			for (Method met : beanmObject.getClass().getDeclaredMethods()) {
				if (met.getName().equals(method.getName())) {
					Class<?>[] params = method.getParameterTypes();
					if (params.length == 1) {
						if (params[0].equals(String.class)) {
							met.invoke(ctx.getApplicationContext().getBean(bean.getClass()), args);

						} else {
							met.invoke(ctx.getApplicationContext().getBean(bean.getClass()),
									mapper.readValue(args.toString(), params[0]));

						}
					}
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

	public synchronized void process(ConsumerRecords<String, String> consumerRecords,
			List<BeanMethodPair> beanMethodPairs) throws Throwable {

		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {

			WaveRecordInfo waveRecordInfo = this.scanHeaders(consumerRecord);
			for (BeanMethodPair pair : beanMethodPairs) {
				for (String SourceName : ioEventService.getSourceNames(pair.getIoEvent())) {
					if (SourceName.equals(waveRecordInfo.getTargetName())) {
						if (pair.getIoEvent().gatewaySource().parallel()) {
							if (this.checkTable(waveRecordInfo, pair.getIoEvent())) {
								ioEventService.sendWaveRecordInfo(waveRecordInfo);
								this.doInvoke(pair.getMethod(), pair.getBean(), consumerRecord.value());
							} 
							else {
								log.info("parallel event arrived : "+waveRecordInfo.getTargetName());
							}

						} else {

							ioEventService.sendWaveRecordInfo(waveRecordInfo);
							log.info(consumerRecord.key());
							this.doInvoke(pair.getMethod(), pair.getBean(), consumerRecord.value());

						}

					}
				}
			}

		}
	}

	private Boolean checkTable(WaveRecordInfo waveRecordInfo, IOEvent ioEvent)
			throws InterruptedException, ExecutionException {
		ParallelEventInfo	parallelEventInfo=new ParallelEventInfo();
		Boolean canInvoke = false;
		String selectInstace = "SELECT * FROM QUERYABLE_parallelEvent WHERE id =\'" + waveRecordInfo.getId() + "\';";
		Row row = KsqlClient.streamQuery(selectInstace).get().poll();
		if (row != null) {
			List<String> arrivedSourceString = parseStringToArray((String) row.getValue("TARGETS"));
			arrivedSourceString.add(waveRecordInfo.getTargetName());
			 parallelEventInfo = new ParallelEventInfo((String) row.getValue("ID"), arrivedSourceString);
			if(ioEventService.sameList(arrivedSourceString, ioEventService.getParalleListSource(ioEvent))) {
				canInvoke=true;
				this.sendParallelEventInfo(parallelEventInfo);	
			}
			else {
				
				parallelEventInfo.setTargets(arrivedSourceString);
				this.sendParallelEventInfo(parallelEventInfo);	

			}
			
		}
		else {
			log.info("row don't exist");
			List<String> list=new ArrayList<String>();
			list.add(waveRecordInfo.getTargetName());
			parallelEventInfo = new ParallelEventInfo(waveRecordInfo.getId(),list);
			this.sendParallelEventInfo(parallelEventInfo);	
		}
	return canInvoke;
	}

	private void sendParallelEventInfo(ParallelEventInfo parallelEventInfo) {
		Message<ParallelEventInfo> message = MessageBuilder.withPayload(parallelEventInfo).setHeader(KafkaHeaders.TOPIC, "parallelEvent")
				.setHeader(KafkaHeaders.MESSAGE_KEY, parallelEventInfo.getId()).setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("source", "customerMS")
				.setHeader("destination", "orderMS").build();

		kafkaTemplate.send(message);		
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

	public WaveRecordInfo scanHeaders(ConsumerRecord<String, String> consumerRecord) {
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo();
		consumerRecord.headers().forEach(header -> {
			if (header.key().equals("targetEvent")) {
				waveRecordInfo.setTargetName(new String(header.value()));
			}
			if (header.key().equals("Correlation_id")) {
				waveRecordInfo.setId(new String(header.value()));
			}
			;
			if (header.key().equals("Process_Name")) {
				waveRecordInfo.setWorkFlowName(new String(header.value()));
			}
		});
		return waveRecordInfo;
	}
}
