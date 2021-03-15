package com.grizzlywave.grizzlywavestarter.configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;
import com.grizzlywave.grizzlywavestarter.model.Order;

/**
 * class where we will declare our aspect for costume annotations
 **/
@Aspect
@Configuration
public class AnnotationAspect {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * method to run when calling our first annotation
	 **/
	@Around("@annotation(com.grizzlywave.grizzlywavestarter.annotations.firstAnnotation)")
	public Object AnnMethode(ProceedingJoinPoint pj) throws Throwable {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		Object obj = pj.proceed();
		LOGGER.info(" annotation first msg");

		LOGGER.info(" annotation second msg");
		return obj;
	}

	/**
	 * publish the order to the broker when we call @waveInit annotation . wave.id()
	 * to access annotation parameters joinPoint.getArgs()[0].toString() to get our
	 * method parameters
	 **/
	@Around(value = "@annotation(anno)", argNames = "jp, anno") // aspect method who have the annotation waveinit
	public Object WaveInitAnnotationProducer(ProceedingJoinPoint joinPoint, WaveInit waveinit) throws Throwable {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveinit.target_topic()).setHeader(KafkaHeaders.MESSAGE_KEY, "999")
				.setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("source", "orderMS")
				.setHeader("orderID", waveinit.id()).setHeader("destination", "customerMS")
				.setHeader("event", waveinit.target_event()).build();
		kafkaTemplate.send(message);
		LOGGER.info("event sent successfully");
		Object[] args = joinPoint.getArgs();
		args[0] = "yes it works";
		Object obj = joinPoint.proceed();
		return obj;
	}

	/* Consumer that we call with @waveTransition annotation */
	@Around(value = "@annotation(anno)", argNames = "jp, anno")
	//@KafkaListener(topics = "order", groupId = "my-group2", containerFactory = "userKafkaListenerFactory")
	public Object receive(ProceedingJoinPoint joinPoint, WaveTransition waveTransition) throws Throwable {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		ThreadLocal threadLocal = new ThreadLocal();
		threadLocal.set("first local thread");
		LOGGER.info(Thread.currentThread().getName());
		LOGGER.info((String)threadLocal.get());

		Object obj = joinPoint.proceed();
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveTransition.target_topic()).setHeader(KafkaHeaders.MESSAGE_KEY, "999")
				.setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("source", waveTransition.source_event())
				.setHeader("destination", waveTransition.target_event())
				.setHeader("event", waveTransition.name()).build();
		kafkaTemplate.send(message);
		LOGGER.info("event  2 sent successfully");
		
		LOGGER.info(joinPoint.getArgs()[0].toString());
		return obj;

	}
}