package com.grizzlywave.grizzlywavestarter.service;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.model.Order;
/**
 * class that we need for our annotation call test : in this class we call the
 * annotation and we will use this class method as a service
 **/
@Component
public class waveInitAnnotation {
	
	@WaveInit(id ="id",target_event= "INIT_ORDER", target_topic="order")
	public void initOrder(Order order){
	}	}