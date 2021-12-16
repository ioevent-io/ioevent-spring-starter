package com.grizzlywave.starter;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.grizzlywave.starter.configuration.properties.WaveProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


import java.util.Properties;


/**
 * Grizzly Wave Starter Main Class
 **/
@Slf4j
@EnableConfigurationProperties(WaveProperties.class)
//@Profile("development")
//@Configuration
//@EnableDiscoveryClient
public class GrizzlyWaveStarterApplication {
//	public static void main(String[] args) {
//
//	  SpringApplication.run(GrizzlyWaveStarterApplication.class, args);
//	}


}
