package com.grizzlywave.grizzlywavestarter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.grizzlywave.grizzlywavestarter.configuration.AnnotationAspect;
import com.grizzlywave.grizzlywavestarter.configuration.KafkaConfig;
import com.grizzlywave.grizzlywavestarter.configuration.KafkaTopicConfiguration;
import com.grizzlywave.grizzlywavestarter.configuration.WaveConfigProperties;
import com.grizzlywave.grizzlywavestarter.configuration.WaveTopicBeanPostProcessor;
import com.grizzlywave.grizzlywavestarter.service.TopicServices;

/**
 * EnableGrizzlyWave annotation allows us to enable the configuration class from
 * the starter in any application that use our starter and this annotation
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ AnnotationAspect.class, KafkaConfig.class, KafkaTopicConfiguration.class, WaveConfigProperties.class,
		TopicServices.class, WaveTopicBeanPostProcessor.class })
public @interface EnableGrizzlyWave {

}
