package com.grizzlywave.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import com.grizzlywave.starter.configuration.properties.WaveProperties;

/**
 * Grizzly Wave Starter Main Class
 **/

@SpringBootApplication
@EnableEurekaClient
@EnableConfigurationProperties(WaveProperties.class)
public class GrizzlyWaveStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
	}
		
}
