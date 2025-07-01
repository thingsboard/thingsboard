/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private static final GenericContainer REDIS = new GenericContainer("bitnami/valkey:8.0")
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
