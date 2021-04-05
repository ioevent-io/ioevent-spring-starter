package com.grizzlywave.grizzlywavestarter.service;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.model.Order;
import com.grizzlywave.grizzlywavestarter.model.WaveResponse;

/**
 * class that we need for our annotation call test : in this class we call the
 * annotation and we will use this class method as a service
 **/
@Component
public class waveInitAnnotation {

	@WaveInit(id = "#order.getId()", target_event = "INIT_ORDER", target_topic = "orderprefix")
	public WaveResponse initOrder(Order order) throws IllegalArgumentException, IllegalAccessException {

		// HashMap<String, Object> hashmap = new HashMap<String, Object>();
		// for (Method method : this.getClass().getMethods()) {
		// hashmap.put("event", method.getAnnotation(WaveInit.class).target_event());
		// return new WaveResponse(order, hashmap);
		// }

		return new WaveResponse(order, new HashMap<String, Object>());
	}

}