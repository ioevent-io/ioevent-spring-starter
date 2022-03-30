package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GatewayOutputEvent annotation allows us to specify the outputs of the task,
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayOutputEvent {

	boolean parallel() default false;

	boolean exclusive() default true;

	OutputEvent[] output() default {};

	String topic() default "";

}
