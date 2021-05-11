package com.grizzlywave.starter.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.WaveTransition;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
/**class service for waveTransition annnotation to test listener handler */
@Service
@WaveWorkFlow(name = "FirstSimpleWorkFlow")
public class TestWaveTransition {

	private ObjectMapper mapper = new ObjectMapper();


	
	@WaveTransition(stepName = "creditReserved", source_event = "customerMS", source_topic = "order", target_event = "orderMS", target_topic = "customer")
	public String  tryWaveTransition(String object)
			throws JsonMappingException, JsonProcessingException {

		return object;

	}



}
