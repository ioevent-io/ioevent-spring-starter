package com.grizzlywave.grizzlywavestarter.annotation;

import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.model.Order;


/**
 * class that we need for our annotation call test : in this class we call the
 * annotation and in the test class we will use this class
 **/
@Component
public class WaveInitAnnotationTest {

	@WaveInit(id ="#order.getId()",target_event= "INIT_ORDER", target_topic="order")
	public void initOrder(Order order){
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		LOGGER.info(order.toString());
	}
}
