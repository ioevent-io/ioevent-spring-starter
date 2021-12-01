package com.grizzlywave.starter.handler;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.grizzlywave.starter.configuration.postprocessor.BeanMethodPair;
import com.grizzlywave.starter.domain.ParallelEventInfo;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;
import com.grizzlywave.starter.service.IOEventService;

@RunWith(SpringRunner.class)
class RecordsHandlerTest {

	@InjectMocks
	RecordsHandler recordsHandler = new RecordsHandler();

	@Mock
	IOEventService ioEventService;

	@Mock
	KafkaTemplate<String, Object> kafkaTemplate;

	@BeforeEach
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	Gson gson = new Gson();

	@Test
	void parseConsumedValueTest() throws JsonMappingException, JsonProcessingException {
		// test String object
		Object string = "test String";
		assertEquals(String.class, recordsHandler.parseConsumedValue(string, String.class).getClass());
		// test custom object
		ParallelEventInfo parallelEventInfo = new ParallelEventInfo("id",
				Arrays.asList("first element ", "second element"));
		Object parallelString = gson.toJson(parallelEventInfo);
		assertEquals(ParallelEventInfo.class,
				recordsHandler.parseConsumedValue(parallelString, ParallelEventInfo.class).getClass());

	}

	@Test
	void parseStringToArrayTest() {

		String listInString = " one, two, three,";
		List<String> listOfStrings = Arrays.asList("one", "two", "three");
		List<String> resultList = recordsHandler.parseStringToArray(listInString);

		assertEquals(listOfStrings, resultList);
	}

	@Test
	void getWaveHeadersTest() {
		ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<String, String>("topic", 1, 152, 11125,
				TimestampType.LOG_APPEND_TIME, null, 0, 0, null, null, new RecordHeaders());
		consumerRecord.headers().add("targetEvent", "target name".getBytes());
		consumerRecord.headers().add("Correlation_id", "id".getBytes());
		consumerRecord.headers().add("Process_Name", "workflow name".getBytes());
		consumerRecord.headers().add("another header", "value".getBytes());

		WaveRecordInfo waveRecordInfoCreated = recordsHandler.getWaveHeaders(consumerRecord);
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("id", "workflow name", "target name", new StopWatch());
		assertEquals(waveRecordInfo.getId(), waveRecordInfoCreated.getId());
		assertEquals(waveRecordInfo.getTargetName(), waveRecordInfoCreated.getTargetName());
		assertEquals(waveRecordInfo.getWorkFlowName(), waveRecordInfoCreated.getWorkFlowName());

	}

	@Test
	void sendParallelInfoTest() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("init", null);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "Target 1", new StopWatch());
		ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<String, String>("topic", 1, 152, 11125,
				TimestampType.LOG_APPEND_TIME, null, 0, 0, null, null, new RecordHeaders());
		consumerRecord.headers().add("targetEvent", "target name".getBytes());
		consumerRecord.headers().add("Correlation_id", "id".getBytes());
		consumerRecord.headers().add("Process_Name", "workflow name".getBytes());
		consumerRecord.headers().add("another header", "value".getBytes());

		WaveParallelEventInformation parallelEventInfo = new WaveParallelEventInformation(consumerRecord,
				waveRecordInfo, new BeanMethodPair(this, method, null), Arrays.asList("Target 1", "Target 2"),
				"appNam");

		Message messageResult = recordsHandler.sendParallelInfo(parallelEventInfo);

		Message<WaveParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ParallelEventTopic")
				.setHeader(KafkaHeaders.MESSAGE_KEY,
						parallelEventInfo.getHeaders().get("Correlation_id") + parallelEventInfo.getSourceRequired())
				.build();

		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));
	}
    
	@Test
	void parallelInvokeMethodTest() throws NoSuchMethodException, SecurityException {
		Method method = this.getClass().getMethod("init", null);
		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
		when(ioEventService.getSourceNames(Mockito.any())).thenReturn(Arrays.asList("Target 1", "Target 2"));
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("1155", "process name", "Target 1", new StopWatch());
		ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<String, String>("topic", 1, 152, 11125,
				TimestampType.LOG_APPEND_TIME, null, 0, 0, null, null, new RecordHeaders());
		consumerRecord.headers().add("targetEvent", "target name".getBytes());
		consumerRecord.headers().add("Correlation_id", "id".getBytes());
		consumerRecord.headers().add("Process_Name", "workflow name".getBytes());
		consumerRecord.headers().add("another header", "value".getBytes());

		recordsHandler.parallelInvoke(new BeanMethodPair(this, method, null), consumerRecord, waveRecordInfo);

		assertThatNoException();

	}


}
