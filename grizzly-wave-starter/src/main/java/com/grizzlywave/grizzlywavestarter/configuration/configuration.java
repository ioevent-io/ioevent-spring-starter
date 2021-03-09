package com.grizzlywave.grizzlywavestarter.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.grizzlywavestarter.service.HelloService;
import com.grizzlywave.grizzlywavestarter.service.HelloServiceImpl;


/*
 * create our auto configuration that will be use in others projects that import our this starter as dependencie
 * */
@Configuration
@ConditionalOnClass(HelloService.class)
public class configuration {
	
	/*
	 * this bean will be created if this bean dose not already exist
	 * this bean it's an example to try our new starter
	 * */
	@Bean
    @ConditionalOnMissingBean
    public HelloService helloService(){

        return new HelloServiceImpl();
    }
}
