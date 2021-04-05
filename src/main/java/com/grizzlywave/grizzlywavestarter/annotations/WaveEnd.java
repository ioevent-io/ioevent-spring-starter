package com.grizzlywave.grizzlywavestarter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WaveEnd {

	String name() default "";

	String source_event();

	String source_topic();

}
