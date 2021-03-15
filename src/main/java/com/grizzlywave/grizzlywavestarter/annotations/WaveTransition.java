package com.grizzlywave.grizzlywavestarter.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WaveTransition {

	String name() default "";
	  String source_event();
	  String source_topic();
	  String target_event();
	  String target_topic();
}
