package com.grizzlywave.starter.annotations.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SourceEvent annotation allows us to specify/define a source event ,
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SourceEvent {

	String name() default "";

	String topic() default "";

}
