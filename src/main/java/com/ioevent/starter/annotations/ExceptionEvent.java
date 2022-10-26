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
 * ExceptionEvent create a Listener which receive business or technical error
 * events from the current task
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionEvent {

	/**
	 * exceptions is an array of classes or interfaces that extends Throwable and
	 * predicted to be occurred in the task method
	 * 
	 * @return array of classes or interfaces that extends Throwable
	 */
	Class<? extends Throwable>[] exception() default {};

	/**
	 * Array of @OutputEvent annotation is used to produce an event which includes a
	 * key of the output and a topic where the event will be produced ( if the topic
	 * is not mentioned the event will be sent to the generic topic specified in
	 * the @IOEvent or @IFlow annotation ).
	 * 
	 * @return Array of @OutputEvent
	 */
	OutputEvent output() default @OutputEvent();

	/**
	 * An @EndEvent annotation define the finishing point of a process with error
	 * which includes a value where we specify the path name to the error end .
	 * 
	 * @return an EndEvent object
	 */
	EndEvent endEvent() default @EndEvent();
}
