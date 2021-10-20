package com.grizzlywave.starter.configuration.eureka;

import com.netflix.discovery.EurekaClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.annotation.Bean;

public class eurekaConfig {

    @Autowired
    EurekaClientConfigBean eurekaClientConfigBean;




    @Bean
    void disableEureka(){
        eurekaClientConfigBean.setEnabled(false);

    }

}
