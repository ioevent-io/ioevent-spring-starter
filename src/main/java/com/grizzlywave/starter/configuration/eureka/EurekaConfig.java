package com.grizzlywave.starter.configuration.eureka;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@ConditionalOnProperty(prefix = "grizzly-wave", name = "eureka.enabled",havingValue = "false",matchIfMissing = true)
@Configuration
@EnableAutoConfiguration(exclude = {EurekaDiscoveryClientConfiguration.class, EurekaClientAutoConfiguration.class})
public class EurekaConfig {
    @Bean
    void showMessage() throws InterruptedException {
        log.info("**** Eureka is disabled ***");
    }
}
