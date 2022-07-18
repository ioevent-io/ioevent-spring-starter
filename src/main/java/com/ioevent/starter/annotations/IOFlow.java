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
 * @IOFlow annotation uses to specify the classes that contains @IOEvent methods
 * , @IOFlow classes are processed by BeanPostProcessors to extract
 * information from @IOEvent method , in @IOFlow we can specify the key
 * or the name of the flow where the class methods are part of ,name of
 * generic topic where the @IOEvent methods will send event if the topic
 * wasn’t specified in the @IOEvent annotation and apiKey which will be
 * attached to the events of the class methods
 *
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface IOFlow {

	String name() default "";

	String key() default "";

	String topic() default "";

	String apiKey() default "";
}
