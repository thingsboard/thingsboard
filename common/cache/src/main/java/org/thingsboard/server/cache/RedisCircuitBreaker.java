// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.ClusterRedirectException;
import org.springframework.data.redis.TooManyClusterRedirectionsException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisRedirectionException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks whether Redis is usable, so consumers can pause instead of failing (and, with the default skip strategy,
 * dropping) every message while Redis is down.
 * <p>
 * Trips to unhealthy after {@code failuresThreshold} availability errors (connection, redirect, cluster-down) within
 * {@code failuresWindowMs}; data errors such as NOSCRIPT or WRONGTYPE do not count. While unhealthy, a probe runs every
 * {@code probeIntervalMs}: in cluster mode it renews the slot map, checks {@code cluster_state:ok} and sends PING to
 * every master through the per-slot connection path, the path that cache writes use. Back to healthy when the probe
 * passes and the breaker has been open for at least {@code minOpenMs}.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
public class RedisCircuitBreaker {

    private static final int MAX_CAUSE_DEPTH = 10;

    private final RedisConnectionFactory connectionFactory;
    private final boolean enabled;
    private final int failuresThreshold;
    private final long failuresWindowMs;
    private final long probeIntervalMs;
    private final long minOpenMs;

    private final Deque<Long> recentFailures = new ArrayDeque<>();
    private volatile boolean healthy = true;
    private volatile long openedAt;
    private ScheduledExecutorService probeExecutor;

    public RedisCircuitBreaker(RedisConnectionFactory connectionFactory,
                               @Value("${redis.circuit_breaker.enabled:false}") boolean enabled,
                               @Value("${redis.circuit_breaker.failures_threshold:5}") int failuresThreshold,
                               @Value("${redis.circuit_breaker.failures_window_ms:10000}") long failuresWindowMs,
                               @Value("${redis.circuit_breaker.probe_interval_ms:1000}") long probeIntervalMs,
                               @Value("${redis.circuit_breaker.min_open_ms:5000}") long minOpenMs) {
        this.connectionFactory = connectionFactory;
        this.enabled = enabled;
        this.failuresThreshold = Math.max(1, failuresThreshold);
        this.failuresWindowMs = failuresWindowMs;
        this.probeIntervalMs = probeIntervalMs;
        this.minOpenMs = minOpenMs;
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            return;
        }
        probeExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("redis-circuit-breaker"));
        probeExecutor.scheduleWithFixedDelay(this::probeIfOpen, probeIntervalMs, probeIntervalMs, TimeUnit.MILLISECONDS);
        log.info("Redis circuit breaker enabled: {} failures within {} ms open it, probe every {} ms, open for at least {} ms",
                failuresThreshold, failuresWindowMs, probeIntervalMs, minOpenMs);
    }

    @PreDestroy
    public void destroy() {
        if (probeExecutor != null) {
            probeExecutor.shutdownNow();
        }
    }

    /**
     * @return {@code false} while the breaker is open, i.e. Redis is considered unavailable
     */
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * Counts the error if it means Redis is unavailable, and opens the breaker when the threshold is reached.
     */
    public void recordFailure(Throwable error) {
        if (!enabled || !healthy || !isAvailabilityError(error)) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (recentFailures) {
            recentFailures.addLast(now);
            while (!recentFailures.isEmpty() && now - recentFailures.peekFirst() > failuresWindowMs) {
                recentFailures.removeFirst();
            }
            if (healthy && recentFailures.size() >= failuresThreshold) {
                recentFailures.clear();
                openedAt = now;
                healthy = false;
                log.warn("Redis is unavailable ({} errors within {} ms), pausing rule engine consumers until it recovers. Last error: {}",
                        failuresThreshold, failuresWindowMs, error.toString());
            }
        }
    }

    void probeIfOpen() {
        if (healthy || System.currentTimeMillis() - openedAt < minOpenMs) {
            return;
        }
        if (probe()) {
            long openMs = System.currentTimeMillis() - openedAt;
            healthy = true;
            log.warn("Redis is available again after {} ms, resuming rule engine consumers", openMs);
        }
    }

    boolean probe() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            if (!(connection instanceof RedisClusterConnection clusterConnection)) {
                connection.ping();
                return true;
            }
            String state = clusterConnection.clusterGetClusterInfo().getState();
            if (!"ok".equals(state)) {
                log.debug("Redis cluster probe failed: cluster_state {}", state);
                return false;
            }
            JedisCluster cluster = ((JedisClusterConnection) clusterConnection).getNativeConnection();
            if (cluster instanceof TbJedisCluster tbCluster) {
                tbCluster.renewSlotCache();
            }
            for (RedisClusterNode node : clusterConnection.clusterGetNodes()) {
                if (!node.isMaster() || node.getSlotRange().getSlots().isEmpty()) {
                    continue;
                }
                int slot = node.getSlotRange().getSlotsArray()[0];
                try (Jedis jedis = new Jedis(cluster.getConnectionFromSlot(slot))) {
                    jedis.ping();
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("Redis probe failed", e);
            return false;
        }
    }

    static boolean isAvailabilityError(Throwable error) {
        Throwable e = error;
        for (int depth = 0; e != null && depth < MAX_CAUSE_DEPTH; depth++, e = e.getCause()) {
            // DataAccessResourceFailureException: RedisConnectionFailureException, and ClusterStateFailureException
            // when no cluster node answers
            if (e instanceof DataAccessResourceFailureException || e instanceof ClusterRedirectException
                    || e instanceof TooManyClusterRedirectionsException || e instanceof JedisConnectionException
                    || e instanceof JedisRedirectionException || e instanceof JedisClusterException
                    || e instanceof JedisClusterOperationException) {
                return true;
            }
        }
        return false;
    }

}
