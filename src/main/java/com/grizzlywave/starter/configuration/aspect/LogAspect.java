package com.grizzlywave.starter.configuration.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.starter.annotations.LogAnnotation;

import lombok.extern.slf4j.Slf4j;

/** aspect to be executed when the annotation method invoked*/
@Slf4j
@Aspect
@Configuration
public class LogAspect {
	
	@Around(value = "@annotation(anno)", argNames = "jp, anno") //
	public Object AnnotationAspect(ProceedingJoinPoint joinPoint, LogAnnotation logAnnotation) throws Throwable {
		log.info("log before the method");
		Object obj = joinPoint.proceed();
		log.info("log after the method");
		return obj;
	}

}
