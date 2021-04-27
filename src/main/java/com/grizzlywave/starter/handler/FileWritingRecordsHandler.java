package com.grizzlywave.starter.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/** Listener handler under dev */
public class FileWritingRecordsHandler implements ConsumerRecordsHandler<String, String> {

	@Autowired
	RecordMessageConverter conv;

	public FileWritingRecordsHandler() {
	}

	@Override
	public void process(final ConsumerRecords<String, String> consumerRecords) {
		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
			consumerRecord.headers().forEach(h -> System.out.println(new String(h.value())));
			System.out.println("--" + consumerRecord.toString());
			System.out.println(consumerRecord.offset() + "--" + consumerRecord.partition());
		}
	}
	/*
	 * public void convert(final ConsumerRecord<String, String> consumerRecord) {
	 * 
	 * conv.toMessage(consumerRecord, acknowledgment, consumer, payloadType); }
	 */
}