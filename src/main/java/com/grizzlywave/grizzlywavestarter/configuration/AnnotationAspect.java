package com.grizzlywave.grizzlywavestarter.configuration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;
import com.grizzlywave.grizzlywavestarter.model.Order;
import com.grizzlywave.grizzlywavestarter.model.WaveResponse;

/**
 * class where we will declare our aspect for costume annotations
 * 
**/
@Aspect
@Configuration
public class AnnotationAspect {

	ExpressionParser parser = new SpelExpressionParser();
	LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

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
	 * Aspect method who have the annotation @waveInit
	 * publish the order to the broker when we call @waveInit annotation . 
	 * wave.id() to access annotation parameters 
	 * joinPoint.getArgs()[0].toString() to get our method parameters
	 **/
	@Around(value = "@annotation(anno)", argNames = "jp, anno") //
	public Object WaveInitAnnotationProducer(ProceedingJoinPoint joinPoint, WaveInit waveinit) throws Throwable {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		UUID uuid= UUID.randomUUID();
		Object[] args = joinPoint.getArgs();
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		String[] params = discoverer.getParameterNames(method);
		EvaluationContext context = new StandardEvaluationContext();
		for (int len = 0; len < params.length; len++) {
			context.setVariable(params[len], args[len]);
		}
		Expression expression = parser.parseExpression(waveinit.id());
		LOGGER.info(expression.getValue(context, String.class));
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveinit.target_topic()).setHeader(KafkaHeaders.MESSAGE_KEY, "999")
				.setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("WorkFlow_ID", uuid).setHeader("source", "orderMS")
				.setHeader("orderID", expression.getValue(context, String.class)).setHeader("destination", waveinit.target_event())
				.setHeader("event", "Start").build();
		kafkaTemplate.send(message);
		LOGGER.info("event sent successfully");
		/*
		 * to proceed the current method with new arguments 
		Object[] newArguments = new Object[1];
		newArguments[0] = new Order(5, 7, 47);
		Object obj = joinPoint.proceed(newArguments);*/
		Object obj = joinPoint.proceed();
		HashMap<String, Object> headers=new HashMap<String, Object>();
		headers.put("destination", waveinit.target_event());
		headers.put("orderID", expression.getValue(context, String.class));
		headers.put("event", "Start");

		obj= new WaveResponse(joinPoint.getArgs()[0], headers);
		return obj;
	}

	/** Consumer that we call with @waveTransition annotation **/
	@Around(value = "@annotation(anno)", argNames = "jp, anno")
	public Object receive(ProceedingJoinPoint joinPoint, WaveTransition waveTransition) throws Throwable {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		Object obj = joinPoint.proceed();
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveTransition.target_topic()).setHeader(KafkaHeaders.MESSAGE_KEY, "999")
				.setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("source", waveTransition.source_event())
				.setHeader("destination", waveTransition.target_event()).setHeader("event", waveTransition.name())
				.build();
		kafkaTemplate.send(message);
		LOGGER.info("event  2 sent successfully");
		LOGGER.info(joinPoint.getArgs()[0].toString());
		return obj;

	}
}