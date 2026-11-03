// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Slf4j
public class RedisJUnit5Test {

    @Container
    private static final GenericContainer REDIS = new GenericContainer("bitnamilegacy/valkey:8.0")
            .withEnv("ALLOW_EMPTY_PASSWORD","yes")
            .withLogConsumer(s -> log.error(((OutputFrame) s).getUtf8String().trim()))
            .withExposedPorts(6379);

    @BeforeAll
    static void beforeAll() {
        log.warn("Starting redis...");
        REDIS.start();
        System.setProperty("cache.type", "redis");
        System.setProperty("redis.connection.type", "standalone");
        System.setProperty("redis.standalone.host", REDIS.getHost());
        System.setProperty("redis.standalone.port", String.valueOf(REDIS.getMappedPort(6379)));

    }

    @AfterAll
    static void afterAll() {
        List.of("cache.type", "redis.connection.type", "redis.standalone.host", "redis.standalone.port")
                .forEach(System.getProperties()::remove);
        REDIS.stop();
        log.warn("Redis is stopped");
    }

    @Test
    void test() {
        assertThat(REDIS.isRunning()).isTrue();
    }

}
