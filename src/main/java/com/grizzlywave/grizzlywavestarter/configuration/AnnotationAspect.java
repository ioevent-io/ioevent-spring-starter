package com.grizzlywave.grizzlywavestarter.configuration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.util.StopWatch;

import com.grizzlywave.grizzlywavestarter.annotations.WaveEnd;
import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;
import com.grizzlywave.grizzlywavestarter.model.WaveResponse;

/**
 * class where we will declare our aspect for costume annotations
 * 
 **/
@Aspect
@Configuration
public class AnnotationAspect {
	private static final Logger log = LoggerFactory.getLogger(AnnotationAspect.class);

	ExpressionParser parser = new SpelExpressionParser();
	LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	WaveConfigProperties waveProperties;

	/**
	 * Aspect method for the annotation @waveInit where it publish an object to the
	 * broker. to access annotation parameters joinPoint.getArgs()[0].toString() to
	 * get our method parameters
	 **/
	@Around(value = "@annotation(anno)", argNames = "jp, anno") //
	public Object WaveInitAnnotationProducer(ProceedingJoinPoint joinPoint, WaveInit waveinit) throws Throwable {
		StopWatch watch = new StopWatch();
		watch.start("waveInit annotation Aspect");
		// Logger LOGGER =
		// Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
		UUID uuid = UUID.randomUUID();
		// get the expression from the annotation
		String event_id = runEpressionWaveInit(joinPoint, waveinit);
		// create the message to produce it in the broker
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + waveinit.target_topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("WorkFlow_ID", uuid).setHeader("source", "orderMS").setHeader("orderID", event_id)
				.setHeader("destination", waveinit.target_event()).setHeader("event", "Start").build();
		kafkaTemplate.send(message);
		log.info("WaveInit event sent successfully ");
		/*
		 * to proceed the current method with new arguments Object[] newArguments = new
		 * Object[1]; newArguments[0] = new Order(5, 7, 47); Object obj =
		 * joinPoint.proceed(newArguments);
		 */
		Object obj = joinPoint.proceed();
		// create WaveResponse
		HashMap<String, Object> headers = new HashMap<String, Object>();
		headers.put("destination", waveinit.target_event());
		headers.put("orderID", event_id);
		headers.put("event", "Start");

		obj = new WaveResponse(joinPoint.getArgs()[0], headers);
		watch.stop();
		log.info(watch.prettyPrint());
		return obj;
	}

	/**
	 * Aspect method using the advice @AfterReturning,after Consuming an object from
	 * the broker and make change on it, @waveTransition annotation publish it again
	 * to another topic
	 **/
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void receive2(JoinPoint joinPoint, WaveTransition waveTransition, Object order) throws Throwable {
		StopWatch watch = new StopWatch();
		watch.start("waveTransition afterReturn  annotation Aspect");
		Message<Object> message = MessageBuilder.withPayload(order)
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + waveTransition.target_topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("source", waveTransition.source_event())
				.setHeader("destination", waveTransition.target_event()).setHeader("event", waveTransition.name())
				.build();
		kafkaTemplate.send(message);
		log.info("WaveTransition event sent successfully");
		log.info(joinPoint.getArgs()[0].toString());
		log.info(order.toString());
		watch.stop();
		log.info(watch.prettyPrint());
	} // return obj;
	/** Consumer that we call with @waveTransition annotation **/

	/** Consumer that we call with @waveTransition annotation **/
	/*
	 * @Around(value = "@annotation(anno)", argNames = "jp, anno") public Object
	 * receive(ProceedingJoinPoint joinPoint, WaveTransition waveTransition) throws
	 * Throwable { StopWatch watch = new StopWatch();
	 * watch.start("waveTransition annotation Aspect"); Object obj =
	 * joinPoint.proceed(joinPoint.getArgs()); Message<Object> message =
	 * MessageBuilder.withPayload(joinPoint.getArgs()[0])
	 * .setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() +
	 * waveTransition.target_topic()) .setHeader(KafkaHeaders.MESSAGE_KEY,
	 * "999").setHeader(KafkaHeaders.PARTITION_ID, 0) .setHeader("source",
	 * waveTransition.source_event()) .setHeader("destination",
	 * waveTransition.target_event()).setHeader("event", waveTransition.name())
	 * .build(); kafkaTemplate.send(message);
	 * log.info("WaveTransition event sent successfully");
	 * log.info(joinPoint.getArgs()[0].toString());
	 * 
	 * HashMap<String, Object> headers = new HashMap<String, Object>();
	 * headers.put("destination", waveTransition.target_event());
	 * headers.put("source", waveTransition.source_event()); headers.put("event",
	 * waveTransition.name());
	 * 
	 * obj = new WaveResponse(joinPoint.getArgs()[0], headers);
	 * log.info(obj.toString()); watch.stop(); log.info(watch.prettyPrint()); return
	 * obj;
	 * 
	 * }
	 */

	/*
	 * @After(value = "@annotation(anno)", argNames = "jp, anno") public void
	 * receive2(JoinPoint joinPoint, WaveTransition waveTransition) throws Throwable
	 * { StopWatch watch=new StopWatch();
	 * watch.start("waveTransition annotation Aspect");
	 * 
	 * // Object obj = joinPoint.proceed(); Message<Object> message =
	 * MessageBuilder.withPayload(joinPoint.getArgs()[0])
	 * .setHeader(KafkaHeaders.TOPIC,
	 * waveTransition.target_topic()).setHeader(KafkaHeaders.MESSAGE_KEY, "999")
	 * .setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("source",
	 * waveTransition.source_event()) .setHeader("destination",
	 * waveTransition.target_event()).setHeader("event", waveTransition.name())
	 * .build(); kafkaTemplate.send(message);
	 * log.info("WaveTransition event sent successfully");
	 * log.info(joinPoint.getArgs()[0].toString()); watch.stop();
	 * log.info(watch.prettyPrint());} //return obj;
	 */
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void receive2(JoinPoint joinPoint, WaveEnd waveEnd, Object order) throws Throwable {
		StopWatch watch = new StopWatch();
		watch.start("waveEnd afterReturn  annotation Aspect");
		Message<Object> message = MessageBuilder.withPayload(order)
				.setHeader(KafkaHeaders.TOPIC, "Wave-End").setHeader(KafkaHeaders.MESSAGE_KEY, "999")
				.setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("source", waveEnd.source_event())
				.setHeader("event", waveEnd.name()).build();
		kafkaTemplate.send(message);
		log.info("WaveTransition event sent successfully");
		log.info(joinPoint.getArgs()[0].toString());
		log.info(order.toString());
		watch.stop();
		log.info(watch.prettyPrint());
	}
	/** @WaveEnd Aspect to close the process **/
/*	@Around(value = "@annotation(anno)", argNames = "jp, anno")
	public Object waveEnd(ProceedingJoinPoint joinPoint, WaveEnd waveEnd) throws Throwable {
		StopWatch watch = new StopWatch();
		watch.start("waveEnd annotation Aspect");
		Object obj = joinPoint.proceed();
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, "Wave-End").setHeader(KafkaHeaders.MESSAGE_KEY, "999")
				.setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("source", waveEnd.source_event())
				.setHeader("event", waveEnd.name()).build();
		kafkaTemplate.send(message);
		log.info("WaveEnd event sent successfully");
		log.info(joinPoint.getArgs()[0].toString());
		watch.stop();
		log.info(watch.prettyPrint());
		return obj;

	}*/

	public String runEpressionWaveInit(ProceedingJoinPoint joinPoint, WaveInit waveinit) {
		String event_id = waveinit.id();
		if (waveinit.id().startsWith("#")) {
			Object[] args = joinPoint.getArgs();
			Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
			String[] params = discoverer.getParameterNames(method);
			EvaluationContext context = new StandardEvaluationContext();
			for (int len = 0; len < params.length; len++) {
				context.setVariable(params[len], args[len]);
			}
			Expression expression = parser.parseExpression(waveinit.id());
			event_id = expression.getValue(context, String.class);
			log.info(expression.getValue(context, String.class));
		}
		return event_id;
	}
}