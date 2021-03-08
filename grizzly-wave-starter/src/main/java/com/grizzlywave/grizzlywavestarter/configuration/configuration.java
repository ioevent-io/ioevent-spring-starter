package com.grizzlywave.grizzlywavestarter.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.grizzlywave.grizzlywavestarter.service.HelloService;
import com.grizzlywave.grizzlywavestarter.service.HelloServiceImpl;



@Configuration
@ConditionalOnClass(HelloService.class)
public class configuration {
	@Bean
    @ConditionalOnMissingBean
    public HelloService helloService(){

        return new HelloServiceImpl();
    }
}
