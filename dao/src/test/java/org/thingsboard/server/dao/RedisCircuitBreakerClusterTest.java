// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.thingsboard.server.cache.CacheSpecs;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisCircuitBreaker;
import org.thingsboard.server.cache.TBRedisClusterConfiguration;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.FstStatsService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.dao.timeseries.TsLatestCacheKey;
import org.thingsboard.server.dao.timeseries.TsLatestRedisCache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

/**
 * The circuit breaker opens when the whole Valkey cluster is down and closes by itself when it is back.
 */
@Slf4j
class RedisCircuitBreakerClusterTest {

    private static final List<Integer> PORTS = List.of(6391, 6392, 6393, 6394, 6395, 6396);
    private static final String NODES = String.join(",", PORTS.stream().map(port -> "127.0.0.1:" + port).toList());
    private static final String IMAGE = "bitnamilegacy/valkey-cluster:8.0";

    private static final List<GenericContainer<?>> CONTAINERS = PORTS.stream()
            .<GenericContainer<?>>map(port -> new GenericContainer<>(IMAGE)
                    .withEnv(Map.of(
                            "VALKEY_CLUSTER_ANNOUNCE_IP", "127.0.0.1",
                            "VALKEY_CLUSTER_DYNAMIC_IPS", "no",
                            "ALLOW_EMPTY_PASSWORD", "yes",
                            "VALKEY_NODES", NODES.replace(",", " "),
                            "VALKEY_PORT_NUMBER", String.valueOf(port)))
                    .withNetworkMode("host"))
            .toList();

    private static JedisConnectionFactory connectionFactory;
    private static TsLatestRedisCache cache;
    private static RedisCircuitBreaker circuitBreaker;

    @BeforeAll
    static void beforeAll() throws Exception {
        CONTAINERS.forEach(GenericContainer::start);
        TimeUnit.SECONDS.sleep(5); // let every node start listening
        var result = CONTAINERS.get(0).execInContainer("/bin/sh", "-c",
                "valkey-cli --cluster create " + NODES.replace(",", " ") + " --cluster-replicas 1 --cluster-yes");
        assertThat(result.getExitCode()).as(result.getStdout() + result.getStderr()).isZero();
        await("cluster ready").atMost(60, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(RedisCircuitBreakerClusterTest::isClusterStable);

        TBRedisClusterConfiguration configuration = new TBRedisClusterConfiguration();
        ReflectionTestUtils.setField(configuration, "clusterNodes", NODES);
        ReflectionTestUtils.setField(configuration, "maxRedirects", 3); // fail a call within ~6 s instead of ~24 s
        ReflectionTestUtils.setField(configuration, "useDefaultPoolConfig", true);
        ReflectionTestUtils.setField(configuration, "topologyRefreshPeriodSec", 0L);
        configuration.setEvictTtlInMs(60000);
        connectionFactory = configuration.loadFactory();
        connectionFactory.afterPropertiesSet();

        circuitBreaker = new RedisCircuitBreaker(connectionFactory, true, 3, 60_000, 500, 1000);
        circuitBreaker.init();

        CacheSpecs specs = new CacheSpecs();
        specs.setMaxSize(10000);
        specs.setTimeToLiveInMinutes(60);
        CacheSpecsMap specsMap = new CacheSpecsMap();
        specsMap.setSpecs(Map.of(CacheConstants.TS_LATEST_CACHE, specs));
        cache = new TsLatestRedisCache(configuration, specsMap, connectionFactory);
        ReflectionTestUtils.setField(cache, "fstStatsService", mock(FstStatsService.class));
        ReflectionTestUtils.setField(cache, "circuitBreaker", circuitBreaker);
    }

    @AfterAll
    static void afterAll() {
        if (circuitBreaker != null) {
            circuitBreaker.destroy();
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        CONTAINERS.forEach(GenericContainer::stop);
    }

    @Test
    void givenWholeClusterDown_whenCacheWritesFail_thenBreakerOpensAndClosesAfterRecovery() throws Exception {
        writeKeys(1);
        assertThat(circuitBreaker.isHealthy()).isTrue();

        // Stop, but keep, every node: each keeps its nodes.conf and rejoins the cluster on start.
        CONTAINERS.forEach(container -> container.getDockerClient().stopContainerCmd(container.getContainerId()).exec());
        ExecutorService writers = Executors.newFixedThreadPool(5);
        try {
            IntStream.range(0, 5).forEach(i -> writers.submit(() -> {
                try {
                    writeKeys(2);
                } catch (Exception e) {
                    log.debug("Expected cache write failure while the cluster is down", e);
                }
            }));
            await("breaker opens").atMost(60, TimeUnit.SECONDS).until(() -> !circuitBreaker.isHealthy());

            TimeUnit.SECONDS.sleep(3);
            assertThat(circuitBreaker.isHealthy()).as("breaker stays open while the cluster is down").isFalse();

            CONTAINERS.forEach(container -> container.getDockerClient().startContainerCmd(container.getContainerId()).exec());
            await("cluster back").atMost(90, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(RedisCircuitBreakerClusterTest::isClusterStable);
            await("breaker closes").atMost(30, TimeUnit.SECONDS).until(circuitBreaker::isHealthy);
        } finally {
            writers.shutdownNow();
        }

        writeKeys(3);
        assertThat(circuitBreaker.isHealthy()).isTrue();
    }

    private static void writeKeys(long version) {
        for (int i = 0; i < 16; i++) {
            cache.put(new TsLatestCacheKey(new DeviceId(UUID.randomUUID()), "temperature"),
                    new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("temperature", version), version));
        }
    }

    private static boolean isClusterStable() {
        return CONTAINERS.stream().allMatch(container -> {
            int port = PORTS.get(CONTAINERS.indexOf(container));
            String replication = exec(container, port, "INFO", "replication");
            return exec(container, port, "CLUSTER", "INFO").contains("cluster_state:ok")
                    && (replication.contains("role:master") || replication.contains("master_link_status:up"));
        });
    }

    @SneakyThrows
    private static String exec(GenericContainer<?> container, int port, String... command) {
        String[] cli = new String[command.length + 3];
        cli[0] = "valkey-cli";
        cli[1] = "-p";
        cli[2] = String.valueOf(port);
        System.arraycopy(command, 0, cli, 3, command.length);
        return container.execInContainer(cli).getStdout();
    }

}
