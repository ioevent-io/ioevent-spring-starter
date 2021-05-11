package com.grizzlywave.starter.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.grizzlywave.starter.annotations.LogAnnotation;

import lombok.extern.slf4j.Slf4j;

/**service with a method annotated with our custom annotation*/

@Slf4j
@Component
public class LogAnnotaionService {
	@Async
	@LogAnnotation
	public void annotatedMethod() {
		log.info("annotated method invoked");
		log.info("this thread "+Thread.currentThread().getId());
	}
}
