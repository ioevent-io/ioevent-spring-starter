package com.grizzlywave.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.grizzlywave.starter.configuration.WaveConfiguration;

/**
 * EnableGrizzlyWave annotation allows us to enable the configuration class from
 * the starter in any application that use our starter and this annotation
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({WaveConfiguration.class})
public @interface EnableGrizzlyWave {

}
