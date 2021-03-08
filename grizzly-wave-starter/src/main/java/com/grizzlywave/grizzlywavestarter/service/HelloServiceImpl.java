package com.grizzlywave.grizzlywavestarter.service;

import java.util.logging.Logger;

import com.grizzlywave.grizzlywavestarter.service.HelloService;

public class HelloServiceImpl implements HelloService {
	@Override
	public void Hello() {
		Logger LOGGER = Logger.getLogger(
			    Thread.currentThread().getStackTrace()[0].getClassName() );
		LOGGER.info("starter msg");
		
	}

}
