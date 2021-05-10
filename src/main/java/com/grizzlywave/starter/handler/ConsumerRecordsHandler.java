package com.grizzlywave.starter.handler;

import java.lang.reflect.Method;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public interface ConsumerRecordsHandler<K, V> {
   void process(ConsumerRecords<K, V> consumerRecords, Object bean, Method method, Consumer<String, String> consumer) throws Exception, Throwable ;
}
