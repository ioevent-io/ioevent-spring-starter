package com.grizzlywave.grizzlywavestarter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.grizzlywave.grizzlywavestarter.model.Order;
import com.grizzlywave.grizzlywavestarter.service.waveInitAnnotation;

@SpringBootApplication
public class GrizzlyWaveStarterApplication {
	@Autowired
    waveInitAnnotation waveInitAnnotation;
	public static void main(String[] args) {
		SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
	}
	@EventListener(ApplicationReadyEvent.class)
	public void shouldLogWaveInitAnnotationMethod() {

		waveInitAnnotation.initOrder(new Order(2, 2, 200));
	}

}
