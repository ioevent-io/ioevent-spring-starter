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
 * OutputEvent annotation is used to produce an event which includes a key of
 * the output and a topic where the event will be produced ( if the topic is not
 * mentioned the event will be sent to the generic topic specified in
 * the @IOEvent or @IFlow annotation ).
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OutputEvent {
	/**
	 * Specify the name of the output event
	 * 
	 * @return the output event name
	 */
	String value() default "";

	/**
	 * The key of the output , which considered as output event name
	 * 
	 * @return the output event name
	 */
	String key() default "";

	/**
	 * The topic name where to produce the event into
	 * 
	 * @return the topic name
	 */
	String topic() default "";

	/**
	 * suffix to be add to the input key consumed to make an output key 
	 * 
	 * @return the suffix name
	 */
	String suffix() default "";
	/**
	 * Specify if the event will be sent to a manual task
	 *
	 */
	boolean userActionRequired() default false;

}
