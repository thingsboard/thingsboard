package com.iot.thingmodel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ThingModelServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThingModelServiceApplication.class, args);
    }
}
