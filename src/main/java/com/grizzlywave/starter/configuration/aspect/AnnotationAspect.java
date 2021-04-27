package com.grizzlywave.starter.configuration.aspect;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
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

import com.grizzlywave.starter.annotations.WaveEnd;
import com.grizzlywave.starter.annotations.WaveInit;
import com.grizzlywave.starter.annotations.WaveTransition;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
import com.grizzlywave.starter.configuration.WaveConfigProperties;
import com.grizzlywave.starter.model.WaveResponse;

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
		/** Map contain informations to be logged **/
		Map<String, Object> logMap = new LinkedHashMap<String, Object>();
		watch.start("waveInit annotation Aspect");
		logMap.put("StartTime", getISODate(new Date()));
		UUID uuid = UUID.randomUUID();
		/** log the id of workflow instance **/
		logMap.put("correlationId", uuid);
		/** log the event type (Annotation type) **/
		logMap.put("EventType", "Init");
		/** log the workFlow Name **/
		logMap.put("WorkFlow", joinPoint.getTarget().getClass().getAnnotation(WaveWorkFlow.class).name());
		/** get the expression from the annotation **/
		String event_id = runEpressionWaveInit(joinPoint, waveinit);

		/** create the message to produce it in the broker **/
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + waveinit.target_topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("WorkFlow_ID", uuid).setHeader("source", "orderMS").setHeader("orderID", event_id)
				.setHeader("destination", waveinit.target_event()).setHeader("event", waveinit.target_event()).build();
		kafkaTemplate.send(message);
		// log.info("WaveInit event sent successfully ");
		/**
		 * to proceed the current method with new arguments Object[] newArguments = new
		 * Object[1]; newArguments[0] = new Order(5, 7, 47); Object obj =
		 * joinPoint.proceed(newArguments);
		 **/
		Object obj = joinPoint.proceed();
		/** create WaveResponse **/
		HashMap<String, Object> headers = new HashMap<String, Object>();
		headers.put("destination", waveinit.target_event());
		headers.put("orderID", event_id);
		headers.put("event", "Start");
		logMap.put("event", waveinit.target_event());
		obj = new WaveResponse(joinPoint.getArgs()[0], headers);
		logMap.put("Payload", joinPoint.getArgs()[0].toString());
		watch.stop();
		logMap.put("EndTime", getISODate(new Date()));
		logMap.put("Duration", watch.getTotalTimeMillis());
		// log.info(watch.prettyPrint());
		log.info(prettyPrint(logMap));
		return obj;
	}

	/**
	 * Aspect method using the advice @AfterReturning,after Consuming an object from
	 * the broker and make change on it, @waveTransition annotation publish it again
	 * to another topic
	 **/
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void receive2(JoinPoint joinPoint, WaveTransition waveTransition, Object object) throws Throwable {
		Map<String, Object> logMap = new LinkedHashMap<String, Object>();
		logMap.put("StartTime", getISODate(new Date()));
		StopWatch watch = new StopWatch();
		watch.start("waveTransition afterReturn  annotation Aspect");
		/** log the id of workflow instance **/
		logMap.put("correlationId", "");
		/** log the event type (Annotation type) **/
		logMap.put("EventType", "Transition");
		/** log the workFlow Name **/
		logMap.put("WorkFlow", joinPoint.getTarget().getClass().getAnnotation(WaveWorkFlow.class).name());
		logMap.put("StepName", waveTransition.stepName());
		Message<Object> message = MessageBuilder.withPayload(object)
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + waveTransition.target_topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("source", waveTransition.source_event())
				.setHeader("destination", waveTransition.target_event()).setHeader("event", waveTransition.stepName())
				.build();
		kafkaTemplate.send(message);
		// log.info("WaveTransition event sent successfully");
		// log.info(joinPoint.getArgs()[0].toString());
		// log.info(object.toString());
		logMap.put("Source event", waveTransition.source_event());
		logMap.put("Target event", waveTransition.target_event());
		logMap.put("Payload", object);
		watch.stop();
		logMap.put("EndTime", getISODate(new Date()));
		logMap.put("Duration", watch.getTotalTimeMillis());
		// log.info(watch.prettyPrint());
		log.info(prettyPrint(logMap));
	} // return obj;

	/**
	 * Aspect method using the advice @AfterReturning,after Consuming an object from
	 * the broker and make change on it, @waveEnd annotation close the workFlow.
	 **/
	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void receive2(JoinPoint joinPoint, WaveEnd waveEnd, Object object) throws Throwable {
		StopWatch watch = new StopWatch();
		Map<String, Object> logMap = new LinkedHashMap<String, Object>();
		logMap.put("StartTime", getISODate(new Date()));
		watch.start("waveEnd afterReturn  annotation Aspect");
		/** log the id of workflow instance **/
		logMap.put("correlationId", "");
		/** log the event type (Annotation type) **/
		logMap.put("EventType", "End");
		/** log the workFlow Name **/
		logMap.put("WorkFlow", joinPoint.getTarget().getClass().getAnnotation(WaveWorkFlow.class).name());
		logMap.put("event", waveEnd.source_event());
		logMap.put("Payload", object);
		watch.stop();
		logMap.put("EndTime", getISODate(new Date()));
		logMap.put("Duration", watch.getTotalTimeMillis());
		// log.info(watch.prettyPrint());
		log.info(prettyPrint(logMap));

	}

	/** @WaveEnd Aspect to close the process **/

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
			// log.info(expression.getValue(context, String.class));
		}
		return event_id;
	}

	public String prettyPrint(Map<String, Object> map) {
		StringBuilder sb = new StringBuilder();
		sb.append('\n');

		sb.append("{");
		sb.append("\n");
		for (Map.Entry mapentry : map.entrySet()) {
			sb.append("\"").append(mapentry.getKey()).append("\" : \"").append(mapentry.getValue().toString())
					.append("\" \n ");
		}
		sb.append("}");

		return sb.toString();
	}

	private static String getISODate(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSS'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat.format(date);
	}

}