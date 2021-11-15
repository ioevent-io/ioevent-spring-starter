package com.grizzlywave.starter.service;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/*
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamService {
	private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
	public String ksearch(String id) {
		 final KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
    final ReadOnlyKeyValueStore<String, String> store =
        kafkaStreams.store(StoreQueryParameters.fromNameAndType("parallel", QueryableStoreTypes.keyValueStore()));
   log.info("stream value !!!!"+store.get(id));
   return    store.get(id);
	}
}
*/