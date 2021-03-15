package com.grizzlywave.grizzlywavestarter.service;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.model.Order;

@Component
public class waveInitAnnotation {
	
	@WaveInit(id ="id",target_event= "INIT_ORDER", target_topic="order")
	public void initOrder(Order order){
	}	}