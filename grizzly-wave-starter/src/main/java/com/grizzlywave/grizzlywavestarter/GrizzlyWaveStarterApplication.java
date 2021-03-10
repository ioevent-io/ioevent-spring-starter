package com.grizzlywave.grizzlywavestarter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.grizzlywave.grizzlywavestarter.model.Order;

@SpringBootApplication
public class GrizzlyWaveStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
	}


}
