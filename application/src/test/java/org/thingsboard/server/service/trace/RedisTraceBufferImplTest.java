package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisTraceBufferImplTest extends AbstractTraceBufferTest {
    @SuppressWarnings("rawtypes")
    private static GenericContainer redis;
    private static JedisConnectionFactory connectionFactory;

    @BeforeAll
    static void startRedis() {
        redis = new GenericContainer<>("bitnamilegacy/valkey:8.0")
                .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                .withExposedPorts(6379);
        redis.start();
        connectionFactory = new JedisConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redis != null) {
            redis.stop();
        }
    }

    @BeforeEach
    void flushRedis() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Override
    protected TraceBuffer createBuffer(int maxSpansPerTrace, long traceTtlMs, StatsFactory statsFactory) {
        return new RedisTraceBufferImpl(connectionFactory, maxSpansPerTrace, traceTtlMs, KEY_TTL_MS, statsFactory);
    }

    private static final long KEY_TTL_MS = 60_000L;

    @Override
    protected boolean expectedShared() {
        return true;
    }

    @Test
    void everyKeyTheDrainScriptTouchesHashesToTheSameClusterSlot() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        int traceCount = 50;
        for (int i = 0; i < traceCount; i++) {
            buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one")));
        }

        List<String> traceKeys = traceHashKeys();
        assertThat(traceKeys).hasSize(traceCount);
        for (String traceKey : traceKeys) {
            String hashTag = traceKey.substring(traceKey.indexOf('{'), traceKey.indexOf('}') + 1);
            String deadlinesKey = "trace:deadlines:" + hashTag;
            assertThat(JedisClusterCRC16.getSlot(traceKey))
                    .as("trace hash [%s] must share a slot with the declared key [%s]", traceKey, deadlinesKey)
                    .isEqualTo(JedisClusterCRC16.getSlot(deadlinesKey));
        }
    }

    @Test
    void appendIssuesOneRoundTripPerPartitionNotPerTrace() {
        int partitionCount = 64;
        int traceCount = 400;
        MeterRegistry registry = new SimpleMeterRegistry();
        TraceBuffer buffer = createBuffer(10, TTL_MS, statsFactory(registry));

        List<BufferedSpan> batch = new ArrayList<>();
        for (int i = 0; i < traceCount; i++) {
            UUID tenantId = UUID.randomUUID();
            String traceId = randomTraceId();
            batch.add(span(tenantId, traceId, "span-1", null, "one"));
            batch.add(span(tenantId, traceId, "span-2", "span-1", "two"));
        }

        buffer.append(batch);

        long roundTrips = registry.find("trace.export.redis.duration").timer().count();
        assertThat(roundTrips)
                .as("%d traces must cost at most %d round trips, not one each", traceCount, partitionCount)
                .isBetween(1L, (long) partitionCount);

        assertThat(drain(buffer)).hasSize(traceCount);
    }

    @Test
    void newTraceHashGetsTheConfiguredExpiry() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one")));

        List<String> keys = traceHashKeys();
        assertThat(keys).hasSize(1);
        assertThat(pttl(keys.get(0)))
                .as("a new trace hash must carry the configured expiry")
                .isBetween(1L, KEY_TTL_MS);
    }

    @Test
    void appendingMoreSpansDoesNotPushTheExpiryOut() throws InterruptedException {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        buffer.append(List.of(span(tenantId, traceId, "span-1", null, "one")));
        String traceKey = traceHashKeys().get(0);
        long afterCreate = pttl(traceKey);

        Thread.sleep(1200);
        buffer.append(List.of(span(tenantId, traceId, "span-2", "span-1", "two")));

        long afterSecondAppend = pttl(traceKey);
        assertThat(afterSecondAppend)
                .as("expiry must keep counting down, not restart at %d ms", KEY_TTL_MS)
                .isLessThan(afterCreate - 1000);
    }

    // The trace hashes ("trace:{trace-pN}:<tenant>:<trace>"), read straight from Redis.
    private static List<String> traceHashKeys() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            return connection.keyCommands().keys("trace:{*}:*".getBytes(StandardCharsets.UTF_8)).stream()
                    .map(key -> new String(key, StandardCharsets.UTF_8))
                    .toList();
        }
    }

    private static long pttl(String key) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Long pttl = connection.keyCommands().pTtl(key.getBytes(StandardCharsets.UTF_8));
            return pttl == null ? -2L : pttl;
        }
    }
}
