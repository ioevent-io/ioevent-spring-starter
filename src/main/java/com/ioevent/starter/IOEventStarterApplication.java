package com.ioevent.starter;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.ioevent.starter.configuration.properties.IOEventProperties;

import lombok.extern.slf4j.Slf4j;


/**
 *  IOEvent Starter Main Class
 **/
@Slf4j
@EnableConfigurationProperties(IOEventProperties.class)
//@Profile("development")
//@Configuration
//@EnableDiscoveryClient
public class IOEventStarterApplication {
//	public static void main(String[] args) {
//
//	  SpringApplication.run(IOEventStarterApplication.class, args);
//	}


}
