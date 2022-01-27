package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EndEvent annotation allows us to specify that the task is an end task,
 **/
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EndEvent {

	String value() default "";

	String key() default "";

}
