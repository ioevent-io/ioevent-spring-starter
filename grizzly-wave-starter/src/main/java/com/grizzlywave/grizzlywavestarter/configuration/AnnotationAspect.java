package com.grizzlywave.grizzlywavestarter.configuration;

import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;


/**
 *class where we will declare our aspect for costume annotations  
 **/
@Aspect
@Configuration
public class AnnotationAspect {

	/**
	 * method to run when calling our first annotation
	 **/
	@Around("@annotation(com.grizzlywave.grizzlywavestarter.annotations.firstAnnotation)")
	public Object AnnMethode(ProceedingJoinPoint pj) throws Throwable {
		Logger LOGGER = Logger.getLogger(
			    Thread.currentThread().getStackTrace()[0].getClassName() );
		LOGGER.info(" annotation first msg");
		Object obj= pj.proceed();
		LOGGER.info(" annotation second msg");
		return obj;
	}
	
}
