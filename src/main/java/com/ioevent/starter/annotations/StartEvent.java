package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * StartEvent annotation allows us to specify that the task is an start task,
 **/
@Target({ ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface StartEvent {
	
	String value() default "";
	
	String key() default "";

}
