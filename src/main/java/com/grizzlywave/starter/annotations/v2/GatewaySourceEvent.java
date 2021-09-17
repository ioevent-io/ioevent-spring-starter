package com.grizzlywave.starter.annotations.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewaySourceEvent {
	
	boolean parallel() default false;
	
	boolean exclusive() default true;
	
	SourceEvent[] source() default {};

	String topic() default "";
	

}
