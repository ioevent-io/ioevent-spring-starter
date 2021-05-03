package com.grizzlywave.starter.configuration.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.WaveEnd;
import com.grizzlywave.starter.annotations.WaveWorkFlow;
import com.grizzlywave.starter.logger.EventLogger;
/**
 * Aspect method using the advice @AfterReturning,after Consuming an object from
 * the broker and make change on it, @waveEnd annotation close the workFlow.
 **/
@Aspect
@Configuration
public class WaveEndAspect {

	private static final Logger log = LoggerFactory.getLogger(WaveEndAspect.class);

	private ObjectMapper mapper = new ObjectMapper();


	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void receive2(JoinPoint joinPoint, WaveEnd waveEnd, Object object) throws Throwable {
		StopWatch watch = new StopWatch();
		EventLogger eventLogger = new EventLogger();
		eventLogger.startEventLog();
		watch.start("waveEnd afterReturn  annotation Aspect");
		String workflow = joinPoint.getTarget().getClass().getAnnotation(WaveWorkFlow.class).name();
		watch.stop();
		eventLogger.setting(null, workflow, waveEnd.stepName(), waveEnd.source_event(), "__", "End", object);
		eventLogger.stopEvent(watch.getTotalTimeMillis());
		String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
		log.info(jsonObject);

	}
}
