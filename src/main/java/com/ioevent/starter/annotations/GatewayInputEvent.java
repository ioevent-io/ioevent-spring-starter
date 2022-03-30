package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GatewayInputEvent annotation allows us to specify the inputs of the task,
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayInputEvent {

	boolean parallel() default false;

	boolean exclusive() default true;

	InputEvent[] input() default {};

	String topic() default "";

}
