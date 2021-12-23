package com.grizzlywave.starter.annotations.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * EndEvent annotation allows us to specify that the task is an start task,
 **/
@Target({ ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface StartEvent {
	
	String value() default "";
	
	String key() default "";

}
