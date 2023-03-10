/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ioevent.starter.enums.EventTypesEnum;

/**
 * Annotation that marks a method of IOEvent, in @IOEvent we can specify the key
 * or the name of the task, name of generic topic where the @IOEvent methods
 * will receive and send an event if the topic wasn’t specified in
 * the @InputEvent and @OutputEvent annotation, it also specify input as list
 * of @InputEvent from where the annotation will receive events and output as
 * list of @OutputEvent where the annotation will send events, it can define the
 * method as Gateway using @GatewayOutputEvent and @GatewayInputEvent, finally
 * he can declare start/end method explicitly using @StartEvent and @EndEvent or
 * declare start/end method implicitly if you don’t mention any input/output.
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IOEvent {

	/**
	 * 
	 * The key of ioevent task define the task name
	 * 
	 * @return the ioevent task key
	 */
	String key() default "";

	/**
	 * The topic name for ioevent task from where to consume events that can invoke
	 * the method or produce event into it after running this ioevent method
	 * 
	 * @return the topic name
	 */
	String topic() default "";

	/**
	 * 
	 * Array of @InputEvent ,specify input as array of @InputEvent which create a
	 * Listener for each input and receive events from the topic ( if the topic is
	 * not mentioned it will listen to the generic topic specified in the @IOEvent
	 * or @IFlow annotation ), and while the listener consumes an event it will
	 * verify if the output key of the received event is equal to the @InputEvent
	 * key in order to invoke the specific method.
	 * 
	 * @return Array of @InputEvent
	 */
	InputEvent[] input() default @InputEvent();

	/**
	 * Returns a @GatewayInputEvent used to converge parallel branches, it waits
	 * until receiving all input branches @InputEvent to execute the ioevent method
	 * and send the event to the @OutputEvent.
	 * 
	 * @return a GatewayInputEvent object
	 */
	GatewayInputEvent gatewayInput() default @GatewayInputEvent();

	/**
	 * Array of @OutputEvent annotation is used to produce an event which includes a
	 * key of the output and a topic where the event will be produced ( if the topic
	 * is not mentioned the event will be sent to the generic topic specified in
	 * the @IOEvent or @IFlow annotation ).
	 * 
	 * @return Array of @OutputEvent
	 */
	OutputEvent[] output() default @OutputEvent();

	/**
	 * Returns a @GatewayOutputEvent used to declare either an exclusive or parallel
	 * gateway .
	 * <p>
	 * In case of exclusive gateway : An exclusive gateway evaluates the state of
	 * the business process and, based on the returned IOResponse key, it breaks the
	 * flow into one of the two or more mutually exclusive paths. we set the value
	 * of exclusive to true and define the list of @OutputEvent where the method
	 * will produce the event to the output with the same key of the IOResponse
	 * output key. In IOResponse we specify the output key and the body to be send
	 * to the event.
	 * <p>
	 * In case of Parallel gateway : it models a fork into multiple paths of
	 * execution ,we set the value of parallel to true and define the list of output
	 * branches @OutputEvent where to produce the event simultaneously.
	 * 
	 * @return a GatewayOutputEvent object
	 */

	GatewayOutputEvent gatewayOutput() default @GatewayOutputEvent();

	/**
	 * A @StartEvent annotation define the starting point of a process which
	 * includes a key where we specify the name of the flow.
	 * 
	 * 
	 * @return a StartEvent object
	 */
	StartEvent startEvent() default @StartEvent();

	/**
	 * An @EndEvent annotation define the finishing point of a process which
	 * includes a key where we specify the name of the flow.
	 * 
	 * @return an EndEvent object
	 */
	EndEvent endEvent() default @EndEvent();

	/**
	 * An @ExceptionEvent is an annotation that defines error handling in two ways :
	 * <p>
	 * We can choose to end the flow with end error, inside the @ExceptionEvent we
	 * specify the list of exceptions predicted to be throwed by the task and we add
	 * the @EndEvent which define the end of the flow with an error.
	 * <p>
	 * Or Create a boundary event to send the error to the handling task : inside
	 * the @ExceptionEvent we specify the list of exceptions predicted to be
	 * occurred in the task and declare the output which is an @OutputEvent where
	 * the payload would be sent to the handling method associated to the output.
	 * 
	 * @return an ExceptionEvent object
	 */
	ExceptionEvent exception() default @ExceptionEvent();
	EventTypesEnum EventType() default EventTypesEnum.SERVICE;
	String textAnnotation() default "";
}
