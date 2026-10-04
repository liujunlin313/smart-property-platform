package com.smartproperty.platform;

import com.smartproperty.platform.infrastructure.localdev.LocalDevelopmentServices;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
public class SmartPropertyApplication {
    public static void main(String[] args) {
        LocalDevelopmentServices.start();
        SpringApplication.run(SmartPropertyApplication.class, args);
    }
}
