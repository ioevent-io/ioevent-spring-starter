package com.grizzlywave.grizzlywavestarter.annotation;

import org.springframework.stereotype.Component;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;
import com.grizzlywave.grizzlywavestarter.model.Order;


	/**
	 * class that we need for our annotation call test : in this class we call the
	 * annotation and in the test class we will use this class
	 **/
	@Component
	public class WaveTransitionAnnotation {


		@WaveTransition(name="cheked",source_event="customerMS",source_topic="order",target_event="orderMS",target_topic="customer")
		public void tryWveTransition(Order order){
		
		}
	}

