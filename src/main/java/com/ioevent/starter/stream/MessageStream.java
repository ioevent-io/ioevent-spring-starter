package com.ioevent.starter.stream;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.ioevent.starter.domain.IOEventMessageEventInformation;

public class MessageStream {

	@Value("${spring.application.name}")
	private String appName;

	/**
	 * method for processing parallel events from the
	 * ioevent-parallel-gateway-events topic using kafka stream,
	 * 
	 * @param builder type of StreamsBuilder,
	 */
	@Autowired
	public void processMessage(final StreamsBuilder builder) {

		Gson gson = new Gson();

		KStream<String, String> kstream = builder
				.stream("ioevent-message-events", Consumed.with(Serdes.String(), Serdes.String())).map(KeyValue::new)
				.filter((k, v) -> {
					IOEventMessageEventInformation value = gson.fromJson(v, IOEventMessageEventInformation.class);
					return appName.equals(value.getHeaders().get("AppName"));
				});
		kstream.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
				.aggregate(() -> "", (key, value, aggregateValue) -> {
					IOEventMessageEventInformation currentValue = gson.fromJson(value,
							IOEventMessageEventInformation.class);
					IOEventMessageEventInformation updatedValue;
					if (!aggregateValue.isBlank()) {
						updatedValue = gson.fromJson(aggregateValue, IOEventMessageEventInformation.class);
					} else {
						updatedValue = currentValue;
					}
					List<String> updatedOutputList = Stream
							.of(currentValue.getInputsArrived(), updatedValue.getInputsArrived())
							.flatMap(Collection::stream).distinct().collect(Collectors.toList());
					Map<String, Object> updatedHeaders = Stream.of(currentValue.getHeaders(), updatedValue.getHeaders())
							.flatMap(map -> map.entrySet().stream())
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
					Map<String, Object> updatedPayload = Stream
							.of(currentValue.getPayloadMap(), updatedValue.getPayloadMap())
							.flatMap(map -> map.entrySet().stream())
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
					if (currentValue.getMessageEventArrived().equals(currentValue.getMessageEventRequired())) {
						updatedValue.setMessageEventArrived(currentValue.getMessageEventArrived());
					}
					updatedOutputList.retainAll(currentValue.getInputRequired());
					updatedValue.setInputsArrived(updatedOutputList);
					updatedValue.setHeaders(updatedHeaders);
					updatedValue.setPayloadMap(updatedPayload);

					aggregateValue = gson.toJson(updatedValue);
					return aggregateValue;
				}).toStream()
				.to("ioevent-event-message-aggregation", Produced.with(Serdes.String(), Serdes.String()));

	}
}
