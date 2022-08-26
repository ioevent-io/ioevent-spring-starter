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

	String key() default "";

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
