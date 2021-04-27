package com.grizzlywave.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import com.grizzlywave.starter.configuration.WaveConfigProperties;

/**
 * Grizzly Wave Starter Main Class
 **/
@SpringBootApplication
@EnableEurekaClient
@EnableConfigurationProperties(WaveConfigProperties.class)
public class GrizzlyWaveStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
	}
		
}
