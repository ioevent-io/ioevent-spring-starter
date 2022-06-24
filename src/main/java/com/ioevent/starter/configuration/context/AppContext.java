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




package com.ioevent.starter.configuration.context;






import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * class to get the current application context
 **/
public class AppContext implements ApplicationContextAware {
	private static ApplicationContext ctx = null;

	/**
	 * method to get the context
	 * 
	 * @return ctx for the ApplicationContext,
	 */
	public static ApplicationContext getApplicationContext() {
		return ctx;
	}

	/**
	 * method for setting the context
	 * 
	 * @param ctx for the context,
	 * @throws BeansException type of Exception,
	 */
	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.ctx = ctx;
	}
}
