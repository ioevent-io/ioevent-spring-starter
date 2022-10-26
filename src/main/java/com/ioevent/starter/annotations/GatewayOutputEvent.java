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
 * GatewayOutputEvent annotation allowsto determine what path is taken through a
 * process that controls the flow of converging Sequence Flows.a single Gateway
 * could have multiple output flows. GatewayOutputEvent can be divided into two
 * types, the Exclusive Gateway and the Parallel Gateway: for the parallel we
 * set the value of parallel to true and define the list of output
 * branches @OutputEvent where to produce the event simultaneously , for the
 * exclusive we set the value of exclusive to true and define the list
 * of @OutputEvent where the method will produce the event to the output with
 * the same key of the IOResponse output key .
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayOutputEvent {

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
	 * Array of @OutputEvent annotation is used to produce an event which includes a
	 * key of the output and a topic where the event will be produced ( if the topic
	 * is not mentioned the event will be sent to the generic topic specified in
	 * the @IOEvent or @IFlow annotation ).
	 * 
	 * @return Array of @OutputEvent
	 */
	OutputEvent[] output() default {};

	/**
	 * The topic name from where to consume events that can invoke the method or
	 * produce event into it after running this ioevent method
	 * 
	 * @return the topic name
	 */
	String topic() default "";

}
