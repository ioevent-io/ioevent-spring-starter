package com.grizzlywave.starter.configuration.aspect;

import java.lang.reflect.Method;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.WaveInit;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.model.WaveResponse;
import com.grizzlywave.starter.model.WaveResponseHeader;

/**
 * Aspect method for the annotation @waveInit where it publish an object to the
 * broker. to access annotation parameters joinPoint.getArgs()[0].toString() to
 * get our method parameters
 **/
@Aspect
@Configuration
public class WaveInitAspect {
	private static final Logger log = LoggerFactory.getLogger(WaveInitAspect.class);

	ExpressionParser parser = new SpelExpressionParser();
	LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WaveProperties waveProperties;

	@Around(value = "@annotation(anno)", argNames = "jp, anno") //
	public Object WaveInitAnnotationProducer(ProceedingJoinPoint joinPoint, WaveInit waveinit) throws Throwable {
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		/** Map contain informations to be logged **/
		watch.start("waveInit annotation Aspect");
		UUID uuid = UUID.randomUUID();
		String workflow = joinPoint.getTarget().getClass().getAnnotation(WaveWorkFlow.class).name();
		/** get the expression from the annotation **/
		String event_id = runEpressionWaveInit(joinPoint, waveinit);
		/** create the message to produce it in the broker **/
		Message<Object> message = MessageBuilder.withPayload(joinPoint.getArgs()[0])
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + waveinit.target_topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("WorkFlow_ID", uuid).setHeader("source", "orderMS").setHeader("orderID", event_id)
				.setHeader("destination", waveinit.target_event()).setHeader("event", waveinit.target_event()).build();
		kafkaTemplate.send(message);
		Object obj = joinPoint.proceed();
		/** create WaveResponse **/
		WaveResponseHeader headers = new WaveResponseHeader(workflow, "___", waveinit.target_event(), "Init");
		obj = new WaveResponse(joinPoint.getArgs()[0], headers);
		watch.stop();
		eventLogger.setting(uuid, workflow, "Start", null, waveinit.target_event(), "Init",
				joinPoint.getArgs()[0].toString());
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
		return obj;
	}

	/**
	 * to proceed the current method with new arguments Object[] newArguments = new
	 * Object[1]; newArguments[0] = new Order(5, 7, 47); Object obj =
	 * joinPoint.proceed(newArguments);
	 **/

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

}