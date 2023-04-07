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
import com.ioevent.starter.domain.IOEventParallelEventInformation;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class ParallelStream {


	@Value("${spring.application.name}")
	private String appName;
	
	/**
	 * method for processing parallel events from the
	 * ioevent-parallel-gateway-events topic using kafka stream,
	 * 
	 * @param builder type of StreamsBuilder,
	 */
	@Autowired
	public void processKStream(final StreamsBuilder builder) {

		Gson gson = new Gson();

		KStream<String, String> kstream = builder
				.stream("ioevent-parallel-gateway-events", Consumed.with(Serdes.String(), Serdes.String()))
				.map(KeyValue::new).filter((k, v) -> {
					IOEventParallelEventInformation value = gson.fromJson(v, IOEventParallelEventInformation.class);
					return appName.equals(value.getHeaders().get("AppName"));
				});
		kstream.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
				.aggregate(() -> "", (key, value, aggregateValue) -> {
					IOEventParallelEventInformation currentValue = gson.fromJson(value,
							IOEventParallelEventInformation.class);
					IOEventParallelEventInformation updatedValue;
					if (!aggregateValue.isBlank()) {
						updatedValue = gson.fromJson(aggregateValue, IOEventParallelEventInformation.class);
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
					updatedValue.setInputsArrived(updatedOutputList);
					updatedValue.setHeaders(updatedHeaders);
					updatedValue.setPayloadMap(updatedPayload);
					aggregateValue = gson.toJson(updatedValue);
					return aggregateValue;
				}).toStream()
				.to("ioevent-parallel-gateway-aggregation", Produced.with(Serdes.String(), Serdes.String()));

	}
}
