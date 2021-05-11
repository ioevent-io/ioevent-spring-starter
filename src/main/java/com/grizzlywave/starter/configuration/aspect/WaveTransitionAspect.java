package com.grizzlywave.starter.configuration.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.WaveTransition;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.logger.EventLogger;

import lombok.extern.slf4j.Slf4j;
/**
 * Aspect method using the advice @AfterReturning,after Consuming an object from
 * the broker and make change on it, @waveTransition annotation publish it again
 * to another topic
 **/
@Slf4j
@Aspect
@Component
public class WaveTransitionAspect {

	ExpressionParser parser = new SpelExpressionParser();
	LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WaveProperties waveProperties;


	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void receive2(JoinPoint joinPoint, WaveTransition waveTransition, Object object) throws Throwable {
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		StopWatch watch = new StopWatch();
		watch.start("waveTransition afterReturn  annotation Aspect");
		String workflow = joinPoint.getTarget().getClass().getAnnotation(WaveWorkFlow.class).name();
		Message<Object> message = MessageBuilder.withPayload(object)
				.setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + waveTransition.target_topic())
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0)
				.setHeader("source", waveTransition.source_event())
				.setHeader("destination", waveTransition.target_event()).setHeader("event", waveTransition.stepName())
				.build();
		kafkaTemplate.send(message);
		watch.stop();
		eventLogger.setting(null, workflow, waveTransition.stepName(), waveTransition.source_event(),
				waveTransition.target_event(), "Transition", object);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);
	}

}
