// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.logexternal;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.cache.logexternal.LogChunkRedisBuffer;

@Testcontainers
@Slf4j
public class LogChunkRedisBufferContractTest extends AbstractLogChunkBufferContractTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("bitnamilegacy/valkey:8.0")
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withLogConsumer(s -> log.debug(((OutputFrame) s).getUtf8String().trim()))
            .withExposedPorts(6379);

    private static JedisConnectionFactory connectionFactory;
    private static LogChunkBuffer buffer;

    @BeforeAll
    static void beforeAll() {
        REDIS.start();
        connectionFactory = new JedisConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
        buffer = new LogChunkRedisBuffer(connectionFactory, 900, MAX_CHUNKS_PER_UNIT);
    }

    @AfterAll
    static void afterAll() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        REDIS.stop();
    }

    @Override
    protected LogChunkBuffer buffer() {
        return buffer;
    }
}
