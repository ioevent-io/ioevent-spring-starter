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
 * GatewayInputEvent annotation allows to determine what path is taken through a
 * process that controls the flow of converging Sequence Flows.a single Gateway
 * could have multiple @InputEvent. ,the Parallel Gateway wait until all
 * inputEvents to arrive so he can join them and run his method.
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayInputEvent {

	/**
	 * If true the gateway type is parallel
	 * 
	 * @return Whether it's an parallel gateway
	 */
	boolean parallel() default false;

	/**
	 * If true the gateway type is exclusive
	 * 
	 * @return Whether it's an exclusive gateway
	 */
	boolean exclusive() default true;

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
	InputEvent[] input() default {};

	/**
	 * The topic name from where to consume events that can invoke the method or
	 * produce event into it after running this ioevent method
	 * 
	 * @return the topic name
	 */
	String topic() default "";

}
