package com.grizzlywave.starter.configuration.aspect.v2;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Configuration
public class IOEventEndAspect {

	private ObjectMapper mapper = new ObjectMapper();

	
	@Autowired
	private IOEventService ioEventService;
	
	@Around(value = "@annotation(anno)", argNames = "jp, anno") //
	public Object iOEventAnnotationAspect(ProceedingJoinPoint joinPoint, IOEvent ioEvent) throws Throwable {
		Object obj = joinPoint.proceed();
		if (!ioEvent.endEvent().key().equals("")) {
			StopWatch watch = new StopWatch();
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			watch.start("IOEvent End annotation Aspect");
			String workflow = ioEvent.endEvent().key();
			watch.stop();
			eventLogger.setting(null, workflow, ioEvent.name(), ioEventService.getSourceNames(ioEvent), "__", "End", joinPoint.getArgs()[0]);
			eventLogger.stopEvent(watch.getTotalTimeMillis());
			String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
			log.info(jsonObject);

		}
		return obj;
	}


}
