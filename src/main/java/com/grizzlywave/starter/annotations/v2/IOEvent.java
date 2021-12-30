package com.grizzlywave.starter.annotations.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IOEvent annotation allows us to define an io event/task ,
 **/
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface IOEvent {
	
	
	String name() default "";
	
	boolean async() default false;
	
	String topic() default "";

    /**
     * 
     * Source event
     */
    SourceEvent[] source() default @SourceEvent();
    
    /**
     * 
     * Source event
     */
    GatewaySourceEvent gatewaySource() default @GatewaySourceEvent();
    
    /**
     * Target Event
     */
    TargetEvent[] target() default @TargetEvent();
    
    /**
     * Gateway Target Event
     */
    
    GatewayTargetEvent gatewayTarget() default @GatewayTargetEvent();
    
    /**
     * Start Event
     */
    StartEvent startEvent() default @StartEvent();
    
    /**
     * End Event
     */
    EndEvent endEvent() default @EndEvent();

}