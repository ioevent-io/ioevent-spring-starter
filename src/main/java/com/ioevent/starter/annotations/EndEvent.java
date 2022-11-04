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
 * EndEvent annotation define the finishing point of a process which includes a
 * key where we specify the name of the flow.
 * 
 * ,
 **/
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EndEvent {
	/**
	 * The name of the IOFlow , which this end event belong to
	 * 
	 * @return the IOFlow name
	 */
	String value() default "";

	/**
	 * The name of the IOFlow , which this end event belong to
	 * 
	 * @return the IOFlow name
	 */
	String key() default "";

}
