package com.grizzlywave.starter.configuration.eureka;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.logging.LogDelegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@Configuration
public class EurekaConfig {

    @Autowired
    private EurekaClient eurekaClient;




    @ConditionalOnExpression( "'${grizzly-wave.eureka}'!='enable'")
    @Bean
    public void  reloadProps() throws InterruptedException {
        log.info("Disabling Eureka Client ...");
        eurekaClient.shutdown();
    }

}
