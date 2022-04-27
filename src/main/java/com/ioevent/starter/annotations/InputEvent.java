package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * InputEvent annotation allows us to specify/define a Input event ,
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface InputEvent {

	String value() default "";

	String key() default "";

	String topic() default "";

}
