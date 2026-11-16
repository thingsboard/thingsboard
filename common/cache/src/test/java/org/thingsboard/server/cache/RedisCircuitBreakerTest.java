// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.ClusterStateFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCircuitBreakerTest {

    private final RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
    private final RedisConnection connection = mock(RedisConnection.class);
    private RedisCircuitBreaker breaker;

    @AfterEach
    void tearDown() {
        if (breaker != null) {
            breaker.destroy();
        }
    }

    @Test
    void givenFailuresBelowThreshold_thenStaysHealthy() {
        breaker = breaker(true, 3, 10_000, 0);

        breaker.recordFailure(connectionError());
        breaker.recordFailure(connectionError());

        assertThat(breaker.isHealthy()).isTrue();
    }

    @Test
    void givenFailuresReachThreshold_thenOpens() {
        breaker = breaker(true, 3, 10_000, 0);

        breaker.recordFailure(connectionError());
        breaker.recordFailure(new InvalidDataAccessApiUsageException("CLUSTERDOWN The cluster is down", new JedisClusterException("CLUSTERDOWN")));
        breaker.recordFailure(new JedisConnectionException("Failed to connect to 10.0.0.1:6379."));

        assertThat(breaker.isHealthy()).isFalse();
    }

    @Test
    void givenNoClusterNodeAnswers_thenCountsAsAvailabilityError() {
        assertThat(RedisCircuitBreaker.isAvailabilityError(new ClusterStateFailureException("Could not retrieve cluster information"))).isTrue();
    }

    @Test
    void givenFailuresOutsideWindow_thenStaysHealthy() throws InterruptedException {
        breaker = breaker(true, 2, 50, 0);

        breaker.recordFailure(connectionError());
        Thread.sleep(100);
        breaker.recordFailure(connectionError());

        assertThat(breaker.isHealthy()).isTrue();
    }

    @Test
    void givenDataErrors_thenStaysHealthy() {
        breaker = breaker(true, 1, 10_000, 0);

        breaker.recordFailure(new InvalidDataAccessApiUsageException("NOSCRIPT", new JedisDataException("NOSCRIPT No matching script.")));
        breaker.recordFailure(new IllegalStateException("serialization failed"));

        assertThat(breaker.isHealthy()).isTrue();
    }

    @Test
    void givenDisabled_thenNeverOpens() {
        breaker = breaker(false, 1, 10_000, 0);

        breaker.recordFailure(connectionError());

        assertThat(breaker.isHealthy()).isTrue();
    }

    @Test
    void givenOpen_whenProbeSucceedsAfterMinOpenTime_thenCloses() {
        breaker = breaker(true, 1, 10_000, 0);
        when(connectionFactory.getConnection()).thenReturn(connection);
        breaker.recordFailure(connectionError());

        breaker.probeIfOpen();

        assertThat(breaker.isHealthy()).isTrue();
    }

    @Test
    void givenOpen_whenProbeFails_thenStaysOpen() {
        breaker = breaker(true, 1, 10_000, 0);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenThrow(connectionError());
        breaker.recordFailure(connectionError());

        breaker.probeIfOpen();

        assertThat(breaker.isHealthy()).isFalse();
    }

    @Test
    void givenOpen_whenMinOpenTimeNotElapsed_thenStaysOpenEvenIfRedisAnswers() {
        breaker = breaker(true, 1, 10_000, 60_000);
        when(connectionFactory.getConnection()).thenReturn(connection);
        breaker.recordFailure(connectionError());

        breaker.probeIfOpen();

        assertThat(breaker.isHealthy()).isFalse();
    }

    private RedisCircuitBreaker breaker(boolean enabled, int threshold, long windowMs, long minOpenMs) {
        // init() is not called: the tests drive probeIfOpen() themselves instead of the scheduler
        return new RedisCircuitBreaker(connectionFactory, enabled, threshold, windowMs, 1000, minOpenMs);
    }

    private static RedisConnectionFailureException connectionError() {
        return new RedisConnectionFailureException("Connection refused", new JedisConnectionException("Connection refused"));
    }

}
