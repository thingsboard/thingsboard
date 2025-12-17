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
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressWarnings("resource") // GenericContainer is closed manually via stop()
public class AbstractRedisClusterContainer {

    static final String NODES =
            "127.0.0.1:6371,127.0.0.1:6372,127.0.0.1:6373," +
                    "127.0.0.1:6374,127.0.0.1:6375,127.0.0.1:6376";

    static final String IMAGE = "bitnamilegacy/valkey-cluster:8.0";

    static final Map<String, String> ENVS = Map.of(
            "VALKEY_CLUSTER_ANNOUNCE_IP", "127.0.0.1",
            "VALKEY_CLUSTER_DYNAMIC_IPS", "no",
            "ALLOW_EMPTY_PASSWORD", "yes",
            "VALKEY_NODES", NODES
    );

    static final GenericContainer<?> redis1 = container("6371");
    static final GenericContainer<?> redis2 = container("6372");
    static final GenericContainer<?> redis3 = container("6373");
    static final GenericContainer<?> redis4 = container("6374");
    static final GenericContainer<?> redis5 = container("6375");
    static final GenericContainer<?> redis6 = container("6376");

    @ClassRule(order = 100)
    public static final ExternalResource resource = new ExternalResource() {

        @Override
        protected void before() throws Throwable {
            redis1.start();
            redis2.start();
            redis3.start();
            redis4.start();
            redis5.start();
            redis6.start();

            Thread.sleep(TimeUnit.SECONDS.toMillis(5));

            String cmd = "valkey-cli --cluster create " +
                    NODES.replace(",", " ") +
                    " --cluster-replicas 1 --cluster-yes";

            log.warn("Init Valkey cluster: {}", cmd);
            var result = redis6.execInContainer("/bin/sh", "-c", cmd);
            Assertions.assertThat(result.getExitCode()).isZero();

            Thread.sleep(TimeUnit.SECONDS.toMillis(5));

            System.setProperty("cache.type", "redis");
            System.setProperty("redis.connection.type", "cluster");
            System.setProperty("redis.cluster.nodes", NODES);
            System.setProperty("redis.cluster.useDefaultPoolConfig", "false");
        }

        @Override
        protected void after() {
            redis1.stop();
            redis2.stop();
            redis3.stop();
            redis4.stop();
            redis5.stop();
            redis6.stop();

            List.of(
                    "cache.type",
                    "redis.connection.type",
                    "redis.cluster.nodes",
                    "redis.cluster.useDefaultPoolConfig"
            ).forEach(System.getProperties()::remove);
        }
    };

    private static GenericContainer<?> container(String port) {
        return new GenericContainer<>(IMAGE)
                .withEnv(ENVS)
                .withEnv("VALKEY_PORT_NUMBER", port)
                .withNetworkMode("host")
                .withLogConsumer(AbstractRedisClusterContainer::consumeLog);
    }

    private static void consumeLog(OutputFrame frame) {
        log.warn(frame.getUtf8StringWithoutLineEnding());
    }
}
