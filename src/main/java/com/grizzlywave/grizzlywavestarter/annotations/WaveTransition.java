package com.grizzlywave.grizzlywavestarter.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation is a kafka @KafkaListener listener that listens to a source
 * event from a kafka topic, initiates processing and finally sends a message to
 * the correct Kafka topic with the headers used to build the BPMN diagram.
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WaveTransition {

	String name() default "";

	String source_event();

	String source_topic();

	String target_event();

	String target_topic();
}
