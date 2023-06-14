///*
// * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//
//
//
//package com.ioevent.starter.handler;
//
//
//
//
//
//
//import static org.assertj.core.api.Assertions.assertThatNoException;
//import static org.junit.Assert.assertEquals;
//import static org.mockito.Mockito.when;
//
//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.List;
//
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.common.header.internals.RecordHeaders;
//import org.apache.kafka.common.record.TimestampType;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.kafka.support.SendResult;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.util.StopWatch;
//import org.springframework.util.concurrent.ListenableFuture;
//import org.springframework.util.concurrent.SettableListenableFuture;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.google.gson.Gson;
//import com.ioevent.starter.configuration.postprocessor.BeanMethodPair;
//import com.ioevent.starter.domain.IOEventHeaders;
//import com.ioevent.starter.domain.IOEventParallelEventInformation;
//import com.ioevent.starter.domain.ParallelEventInfo;
//import com.ioevent.starter.service.IOEventService;
//
//@RunWith(SpringRunner.class)
//class RecordsHandlerTest {
//
//	@InjectMocks
//	RecordsHandler recordsHandler = new RecordsHandler();
//
//	@Mock
//	IOEventService ioEventService;
//
//	@Mock
//	KafkaTemplate<String, Object> kafkaTemplate;
//
//	@BeforeEach
//	public void init() {
//		MockitoAnnotations.initMocks(this);
//	}
//
//	Gson gson = new Gson();
//
//	@Test
//	void parseConsumedValueTest() throws JsonMappingException, JsonProcessingException {
//		// test String object
//		Object string = "test String";
//		assertEquals(String.class, recordsHandler.parseConsumedValue(string, String.class).getClass());
//		// test custom object
//		ParallelEventInfo parallelEventInfo = new ParallelEventInfo("id",
//				Arrays.asList("first element ", "second element"));
//		Object parallelString = gson.toJson(parallelEventInfo);
//		assertEquals(ParallelEventInfo.class,
//				recordsHandler.parseConsumedValue(parallelString, ParallelEventInfo.class).getClass());
//
//	}
//
//	@Test
//	void parseStringToArrayTest() {
//
//		String listInString = " one, two, three,";
//		List<String> listOfStrings = Arrays.asList("one", "two", "three");
//		List<String> resultList = recordsHandler.parseStringToArray(listInString);
//
//		assertEquals(listOfStrings, resultList);
//	}
//
//	@Test
//	void getIOEventHeadersTest() {
//		ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<String, String>("topic", 1, 152, 11125,
//				TimestampType.LOG_APPEND_TIME, null, 0, 0, null, null, new RecordHeaders());
//		consumerRecord.headers().add(IOEventHeaders.OUTPUT_EVENT.toString(), "output name".getBytes());
//		consumerRecord.headers().add(IOEventHeaders.CORRELATION_ID.toString(), "id".getBytes());
//		consumerRecord.headers().add(IOEventHeaders.PROCESS_NAME.toString(), "workflow name".getBytes());
//		consumerRecord.headers().add("another header", "value".getBytes());
//
//		IOEventRecordInfo ioeventRecordInfoCreated = recordsHandler.getIOEventHeaders(consumerRecord);
//		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("id", "workflow name", "output name", new StopWatch(),1000L,null);
//		assertEquals(ioeventRecordInfo.getId(), ioeventRecordInfoCreated.getId());
//		assertEquals(ioeventRecordInfo.getOutputConsumedName(), ioeventRecordInfoCreated.getOutputConsumedName());
//		assertEquals(ioeventRecordInfo.getWorkFlowName(), ioeventRecordInfoCreated.getWorkFlowName());
//
//	}
//
//	@Test
//	void sendParallelInfoTest() throws NoSuchMethodException, SecurityException {
//		Method method = this.getClass().getMethod("init", null);
//		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
//		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
//		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "Output 1", new StopWatch(),1000L,null);
//		ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<String, String>("topic", 1, 152, 11125,
//				TimestampType.LOG_APPEND_TIME, null, 0, 0, null, null, new RecordHeaders());
//		consumerRecord.headers().add(IOEventHeaders.OUTPUT_EVENT.toString(), "output name".getBytes());
//		consumerRecord.headers().add(IOEventHeaders.CORRELATION_ID.toString(), "id".getBytes());
//		consumerRecord.headers().add(IOEventHeaders.PROCESS_NAME.toString(), "workflow name".getBytes());
//		consumerRecord.headers().add("another header", "value".getBytes());
//
//		IOEventParallelEventInformation parallelEventInfo = new IOEventParallelEventInformation(consumerRecord,
//				ioeventRecordInfo, new BeanMethodPair(this, method, null), Arrays.asList("Output 1", "Output 2"),
//				"appNam");
//
//		Message messageResult = recordsHandler.sendParallelInfo(parallelEventInfo);
//
//		Message<IOEventParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
//				.setHeader(KafkaHeaders.TOPIC, "ioevent-parallel-gateway-events")
//				.setHeader(KafkaHeaders.MESSAGE_KEY,
//						parallelEventInfo.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString()).toString() + parallelEventInfo.getInputRequired())
//				.build();
//
//		assertEquals(message.getHeaders().get("kafka_messageKey"), messageResult.getHeaders().get("kafka_messageKey"));
//	}
//    
//	@Test
//	void parallelInvokeMethodTest() throws NoSuchMethodException, SecurityException {
//		Method method = this.getClass().getMethod("init", null);
//		ListenableFuture<SendResult<String, Object>> future = new SettableListenableFuture<>();
//		when(kafkaTemplate.send(Mockito.any(Message.class))).thenReturn(future);
//		when(ioEventService.getInputNames(Mockito.any())).thenReturn(Arrays.asList("Output 1", "Output 2"));
//		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("1155", "process name", "Output 1", new StopWatch(),1000L,null);
//		ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<String, String>("topic", 1, 152, 11125,
//				TimestampType.LOG_APPEND_TIME, null, 0, 0, null, null, new RecordHeaders());
//		consumerRecord.headers().add(IOEventHeaders.OUTPUT_EVENT.toString(), "output name".getBytes());
//		consumerRecord.headers().add(IOEventHeaders.CORRELATION_ID.toString(), "id".getBytes());
//		consumerRecord.headers().add(IOEventHeaders.PROCESS_NAME.toString(), "workflow name".getBytes());
//		consumerRecord.headers().add("another header", "value".getBytes());
//
//		recordsHandler.parallelInvoke(new BeanMethodPair(this, method, null), consumerRecord, ioeventRecordInfo);
//
//		assertThatNoException();
//
//	}
//
//
//}
