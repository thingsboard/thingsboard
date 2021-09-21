package org.thingsboard.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KnockerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnockerServiceApplication.class, args);
    }

}
