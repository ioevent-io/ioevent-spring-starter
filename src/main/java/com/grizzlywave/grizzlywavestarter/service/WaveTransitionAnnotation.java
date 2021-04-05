package com.grizzlywave.grizzlywavestarter.service;

import java.util.logging.Logger;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;
import com.grizzlywave.grizzlywavestarter.model.Order;

/**
 * class that we need for our annotation call test : in this class we call the
 * annotation and in the test class we will use this class
 **/
@Service
public class WaveTransitionAnnotation {

	@WaveTransition(name = "cheked", source_event = "customerMS", source_topic = "order", target_event = "orderMS", target_topic = "customer")
	@KafkaListener(topics = "wave-order", groupId = "my-group2", containerFactory = "userKafkaListenerFactory")
	public void tryWveTransition(@Payload String data) throws JsonMappingException, JsonProcessingException {

		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		LOGGER.info(Thread.currentThread().getName());
		ObjectMapper mapper = new ObjectMapper();
		Order order = mapper.readValue(data, Order.class);
		System.out.println(order.toString());

	}
}
