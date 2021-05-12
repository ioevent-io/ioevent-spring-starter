package com.grizzlywave.starter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.service.LogAnnotaionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Grizzly Wave Starter Main Class
 **/
@Slf4j
@SpringBootApplication
//@EnableEurekaClient
@EnableConfigurationProperties(WaveProperties.class)
public class GrizzlyWaveStarterApplication {

	public static void main(String[] args) {
	  SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
	}
	@Autowired
	 private ApplicationContext applicationContext;
	@Autowired
	private LogAnnotaionService logAnnitaionService;
	
	//get the service and invoke the method inside it
	@EventListener(ApplicationReadyEvent.class)
	public void invokeServiceMethod()
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object myService = applicationContext.getBean(logAnnitaionService.getClass());
		for (Method method : myService.getClass().getDeclaredMethods()) {
			if (method.getName().equals("annotatedMethod")) {		
				method.invoke(myService, null);
				log.info("this thread "+Thread.currentThread().getId());
			}
		}
		
	}
		
}
