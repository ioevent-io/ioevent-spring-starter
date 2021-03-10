package com.test.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.grizzlywave.grizzlywavestarter.annotations.firstAnnotation;
import com.grizzlywave.grizzlywavestarter.model.Order;
import com.grizzlywave.grizzlywavestarter.service.HelloService;
import com.test.starter.service.waveInitAnnotation;



@SpringBootApplication
public class TestStarter1Application implements CommandLineRunner{
	@Autowired
	HelloService helloservice;

	public static void main(String[] args) {
		SpringApplication.run(TestStarter1Application.class, args);

	}
	@Autowired
	waveInitAnnotation waveInitAnnotation;
	
/**
 * method that use our custom  annotation @waveinit to produce an event
 **/
	@EventListener(ApplicationReadyEvent.class)
	public void shouldLogWaveInitAnnotationMethod() {

		waveInitAnnotation.initOrder(new Order(2, 2, 200));
	}
	/**
	 * use our first custom annotation example
	 **/
	@firstAnnotation
	@Override
	public void run(String... strings) throws Exception {
		helloservice.Hello();
	}
}
