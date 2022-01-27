package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.ioevent.starter.configuration.IOEventConfiguration;

/**
 * EnableIOEvent annotation allows us to enable the configuration class from
 * the starter in any application that use our starter and this annotation
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({IOEventConfiguration.class})
public @interface EnableIOEvent {

}
