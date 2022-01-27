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