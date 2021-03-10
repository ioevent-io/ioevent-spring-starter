package com.grizzlywave.grizzlywavestarter.configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;
import com.grizzlywave.grizzlywavestarter.model.Order;

/**
 * class where we will declare our aspect for costume annotations
 **/
@Aspect
@Configuration
public class AnnotationAspect {

	/**
	 * method to run when calling our first annotation
	 **/
	@Around("@annotation(com.grizzlywave.grizzlywavestarter.annotations.firstAnnotation)")
	public Object AnnMethode(ProceedingJoinPoint pj) throws Throwable {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		LOGGER.info(" annotation first msg");

		LOGGER.info(" annotation second msg");
		Object obj = pj.proceed();
		return obj;
	}

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;


	/**
	 * publish the order to the broker when we call @waveInit annotation
	 **/ 
	@Around(value = "@annotation(anno)", argNames = "jp, anno") // aspect method who have the annotation waveinit
	public Object producer(ProceedingJoinPoint joinPoint, WaveInit waveinit) throws Throwable {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
//		wave.id() to access annotation parameters
//		 joinPoint.getArgs()[0].toString() to get our method parameters
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveinit.target_topic()).setHeader(KafkaHeaders.MESSAGE_KEY, "999")
				.setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("source", "orderMS")
				.setHeader("orderID", waveinit.id()).setHeader("destination", "customerMS")
				.setHeader("event", waveinit.target_event()).build();
		kafkaTemplate.send(message);
		LOGGER.info("event sent successfully");
		Object obj = joinPoint.proceed(joinPoint.getArgs());
		return obj; 
	}
	
	/**/}
