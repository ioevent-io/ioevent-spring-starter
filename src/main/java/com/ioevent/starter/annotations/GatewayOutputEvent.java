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
 * @GatewayOutputEvent annotation allowsto determine what path is taken through
 *                     a process that controls the flow of converging Sequence
 *                     Flows.a single Gateway could have multiple output flows.
 * @GatewayOutputEvent can be divided into two types, the Exclusive Gateway and
 *                     the Parallel Gateway: for the parallel we set the value
 *                     of parallel to true and define the list of output
 *                     branches @OutputEvent where to produce the event
 *                     simultaneously , for the exclusive we set the value of
 *                     exclusive to true and define the list of @OutputEvent
 *                     where the method will produce the event to the output
 *                     with the same key of the IOResponse output key .
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayOutputEvent {

	boolean parallel() default false;

	boolean exclusive() default true;

	OutputEvent[] output() default {};

	String topic() default "";

}
