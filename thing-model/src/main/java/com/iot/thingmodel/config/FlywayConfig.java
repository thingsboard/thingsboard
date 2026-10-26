package com.iot.thingmodel.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DatabaseGuard databaseGuard) {
        return flyway -> {
            databaseGuard.verify();
            flyway.migrate();
        };
    }
}
