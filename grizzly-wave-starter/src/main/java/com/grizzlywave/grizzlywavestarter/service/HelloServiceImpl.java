package com.grizzlywave.grizzlywavestarter.service;

import java.util.logging.Logger;

import com.grizzlywave.grizzlywavestarter.service.HelloService;



/*
 * service implemented  to try our new starter
 * 
 * */
public class HelloServiceImpl implements HelloService {
	
	
	/*
	 * method to display a message from our starter 
	 */
	@Override
	public String Hello() {
		Logger LOGGER = Logger.getLogger(
			    Thread.currentThread().getStackTrace()[0].getClassName() );
		LOGGER.info("starter msg");
		return "starter msg sent";
	}

}
