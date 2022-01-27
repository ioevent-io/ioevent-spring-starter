package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GatewayTargetEvent annotation allows us to specify the targets of the task,
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayTargetEvent {

	boolean parallel() default false;

	boolean exclusive() default true;

	TargetEvent[] target() default {};

	String topic() default "";

}
