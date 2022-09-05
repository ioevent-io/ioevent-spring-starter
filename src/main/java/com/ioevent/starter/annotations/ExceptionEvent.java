package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ExceptionEvent create a Listener which receive business or technical error events from the current task 
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionEvent {
	Class<? extends Throwable>[] exception() default {} ;

	OutputEvent output() default @OutputEvent();

	EndEvent endEvent() default @EndEvent();
}
