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
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.List;

@Slf4j
public class AbstractRedisContainer {

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {

        private GenericContainer<?> redis;

        @Override
        protected void before() {
            redis = new GenericContainer<>("bitnamilegacy/valkey:8.0")
                    .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                    .withExposedPorts(6379);

            redis.start();

            System.setProperty("cache.type", "redis");
            System.setProperty("redis.connection.type", "standalone");
            System.setProperty("redis.standalone.host", redis.getHost());
            System.setProperty("redis.standalone.port",
                    String.valueOf(redis.getMappedPort(6379)));
        }

        @Override
        protected void after() {
            if (redis != null) {
                redis.stop();
            }
            List.of(
                    "cache.type",
                    "redis.connection.type",
                    "redis.standalone.host",
                    "redis.standalone.port"
            ).forEach(System.getProperties()::remove);
        }
    };
}
