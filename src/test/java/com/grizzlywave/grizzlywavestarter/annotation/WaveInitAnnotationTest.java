package com.grizzlywave.grizzlywavestarter.annotation;

import java.util.logging.Logger;

import org.aspectj.lang.Aspects;
import org.aspectj.weaver.AjAttribute.Aspect;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.model.Order;


/**
 * class that we need for our annotation call test : in this class we call the
 * annotation and in the test class we will use this class
 **/
@Component
public class WaveInitAnnotationTest {

	@WaveInit(id ="id",target_event= "INIT_ORDER", target_topic="order")
	public void initOrder(Order order){
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		LOGGER.info(order.toString());
	}
}
