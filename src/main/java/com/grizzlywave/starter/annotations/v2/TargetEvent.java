package com.grizzlywave.starter.annotations.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * IOEvent annotation allows us to specify/define a target event ,
 **/
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetEvent {

	String name() default "";

	String topic() default "";

	String suffix() default "";

}
