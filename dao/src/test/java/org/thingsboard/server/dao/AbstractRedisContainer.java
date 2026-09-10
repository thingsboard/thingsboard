// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.List;

@Slf4j
public class AbstractRedisContainer {

    @ClassRule(order = 0)
    public static GenericContainer redis = new GenericContainer("bitnamilegacy/valkey:8.0")
            .withEnv("ALLOW_EMPTY_PASSWORD","yes")
            .withLogConsumer(s -> log.warn(((OutputFrame) s).getUtf8String().trim()))
            .withExposedPorts(6379);

    @ClassRule(order = 1)
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            redis.start();
            System.setProperty("cache.type", "redis");
            System.setProperty("redis.connection.type", "standalone");
            System.setProperty("redis.standalone.host", redis.getHost());
            System.setProperty("redis.standalone.port", String.valueOf(redis.getMappedPort(6379)));
        }

        @Override
        protected void after() {
            redis.stop();
            List.of("cache.type", "redis.connection.type", "redis.standalone.host", "redis.standalone.port")
                    .forEach(System.getProperties()::remove);
        }
    };

}
