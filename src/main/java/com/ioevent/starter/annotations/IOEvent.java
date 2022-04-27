package com.ioevent.starter.annotations;

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
	
	
	String key() default "";
	
	boolean async() default false;
	
	String topic() default "";

    /**
     * 
     * Input event
     */
    InputEvent[] input() default @InputEvent();
    
    /**
     * 
     * Input event
     */
    GatewayInputEvent gatewayInput() default @GatewayInputEvent();
    
    /**
     * Output Event
     */
    OutputEvent[] output() default @OutputEvent();
    
    /**
     * Gateway Output Event
     */
    
    GatewayOutputEvent gatewayOutput() default @GatewayOutputEvent();
    
    /**
     * Start Event
     */
    StartEvent startEvent() default @StartEvent();
    
    /**
     * End Event
     */
    EndEvent endEvent() default @EndEvent();

}