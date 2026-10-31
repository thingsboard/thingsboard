// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.thingsboard.server.cache.CacheSpecs;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisClusterConfiguration;
import org.thingsboard.server.cache.TbCacheTransaction;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.FstStatsService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TsLatestCacheKey;
import org.thingsboard.server.dao.timeseries.TsLatestRedisCache;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

/**
 * A Valkey cluster topology change must not break latest-value cache writes until the JVM restarts.
 * <p>
 * Each test first writes every key, so the Jedis slot map points at the current masters. Then it changes the
 * topology and checks that plain writes (a versioned Lua script) and transactional writes (WATCH/MULTI on the slot
 * owner) work again right away, with the background slot map refresh off. Two cases:
 * <ul>
 *     <li>failover: the old master stays alive as a replica and answers MOVED;</li>
 *     <li>node gone: the old master's address no longer accepts connections, as when a Kubernetes pod gets a new IP.</li>
 * </ul>
 */
@Slf4j
class RedisClusterTopologyChangeTest {

    private static final List<Integer> PORTS = List.of(6381, 6382, 6383, 6384, 6385, 6386);
    private static final String NODES = String.join(",", PORTS.stream().map(port -> "127.0.0.1:" + port).toList());
    private static final String IMAGE = "bitnamilegacy/valkey-cluster:8.0";
    private static final int KEY_COUNT = 64; // enough keys to cover the slots of every master

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

    private static final List<TsLatestCacheKey> KEYS = IntStream.range(0, KEY_COUNT)
            .mapToObj(i -> new TsLatestCacheKey(new DeviceId(UUID.randomUUID()), "temperature"))
            .toList();
    private static final AtomicLong VERSION = new AtomicLong();

    private static JedisConnectionFactory connectionFactory;
    private static TsLatestRedisCache cache;

    @BeforeAll
    static void beforeAll() throws Exception {
        CONTAINERS.forEach(GenericContainer::start);
        TimeUnit.SECONDS.sleep(5); // let every node start listening
        var result = CONTAINERS.get(0).execInContainer("/bin/sh", "-c",
                "valkey-cli --cluster create " + NODES.replace(",", " ") + " --cluster-replicas 1 --cluster-yes");
        assertThat(result.getExitCode()).as(result.getStdout() + result.getStderr()).isZero();
        // Detect a stopped master in 2 s instead of the default 15 s, so its replica takes over sooner.
        PORTS.forEach(port -> exec(containerOf(port), port, "CONFIG", "SET", "cluster-node-timeout", "2000"));
        await("cluster ready").atMost(60, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(RedisClusterTopologyChangeTest::isClusterStable);

        TBRedisClusterConfiguration configuration = new TBRedisClusterConfiguration();
        ReflectionTestUtils.setField(configuration, "clusterNodes", NODES);
        ReflectionTestUtils.setField(configuration, "maxRedirects", 12);
        ReflectionTestUtils.setField(configuration, "useDefaultPoolConfig", true);
        ReflectionTestUtils.setField(configuration, "topologyRefreshPeriodSec", 0L); // test the recovery on the failed call alone
        configuration.setEvictTtlInMs(60000);
        connectionFactory = configuration.loadFactory();
        connectionFactory.afterPropertiesSet();

        CacheSpecs specs = new CacheSpecs();
        specs.setMaxSize(10000);
        specs.setTimeToLiveInMinutes(60);
        CacheSpecsMap specsMap = new CacheSpecsMap();
        specsMap.setSpecs(Map.of(CacheConstants.TS_LATEST_CACHE, specs));
        cache = new TsLatestRedisCache(configuration, specsMap, connectionFactory);
        ReflectionTestUtils.setField(cache, "fstStatsService", mock(FstStatsService.class));
    }

    @AfterAll
    static void afterAll() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        CONTAINERS.forEach(GenericContainer::stop);
    }

    @ParameterizedTest
    @EnumSource(WritePath.class)
    void givenWarmSlotMap_whenMasterFailsOver_thenWritesWorkWithoutRestart(WritePath firstWritePath) throws Exception {
        await("cluster stable").atMost(60, TimeUnit.SECONDS).until(RedisClusterTopologyChangeTest::isClusterStable);
        writeAndVerifyAllKeys(WritePath.SCRIPT); // point the Jedis slot map at the current masters
        ClusterNode master = pickMasterWithLiveReplica();
        ClusterNode replica = liveReplicaOf(master);
        log.info("Failing over master {} to replica {}", master.port(), replica.port());

        var result = containerOf(replica.port()).execInContainer("valkey-cli", "-p", String.valueOf(replica.port()), "CLUSTER", "FAILOVER");
        assertThat(result.getStdout()).contains("OK");
        await("replica promoted").atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> isMaster(replica.port()) && isClusterStable());

        writeAndVerifyAfterTopologyChange(firstWritePath);
        writeAndVerifyAfterTopologyChange(firstWritePath.other());
    }

    @ParameterizedTest
    @EnumSource(WritePath.class)
    void givenWarmSlotMap_whenMasterNodeIsGone_thenWritesWorkWithoutRestart(WritePath firstWritePath) throws Exception {
        await("cluster stable").atMost(60, TimeUnit.SECONDS).until(RedisClusterTopologyChangeTest::isClusterStable);
        writeAndVerifyAllKeys(WritePath.SCRIPT); // point the Jedis slot map at the current masters
        ClusterNode master = pickMasterWithLiveReplica();
        ClusterNode replica = liveReplicaOf(master);
        log.info("Stopping master {}, its replica {} takes over", master.port(), replica.port());

        containerOf(master.port()).stop();
        await("replica promoted").atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> isMaster(replica.port()) && isClusterStable());

        writeAndVerifyAfterTopologyChange(firstWritePath);
        writeAndVerifyAfterTopologyChange(firstWritePath.other());
    }

    /**
     * The two ways the cache writes to a slot owner. The first one to run after a topology change meets the stale
     * slot map; once it renews the map, the other one finds it already fresh. So each case runs once per path first.
     */
    enum WritePath {
        SCRIPT, // versioned put: Lua script sent through the cluster client
        TRANSACTION; // WATCH/MULTI on a connection to the slot owner

        WritePath other() {
            return this == SCRIPT ? TRANSACTION : SCRIPT;
        }
    }

    /**
     * No write may throw. A transaction may still be not applied once per promoted node: that node does not have the
     * versioned-put script yet, the transaction loads it and reports a miss. So the first pass only has to complete,
     * and the second pass must store every key.
     */
    private static void writeAndVerifyAfterTopologyChange(WritePath writePath) {
        writeAllKeys(writePath, VERSION.incrementAndGet());
        writeAndVerifyAllKeys(writePath);
    }

    private static void writeAndVerifyAllKeys(WritePath writePath) {
        long version = VERSION.incrementAndGet();
        writeAllKeys(writePath, version);
        verifyAllKeys(version);
    }

    private static void writeAllKeys(WritePath writePath, long version) {
        for (TsLatestCacheKey key : KEYS) {
            if (writePath == WritePath.SCRIPT) {
                cache.put(key, entry(version));
            } else {
                TbCacheTransaction<TsLatestCacheKey, TsKvEntry> transaction = cache.newTransactionForKey(key);
                transaction.put(key, entry(version));
                transaction.commit();
            }
        }
    }

    private static void verifyAllKeys(long version) {
        for (TsLatestCacheKey key : KEYS) {
            var cached = cache.get(key);
            assertThat(cached).as("cached value of %s", key).isNotNull();
            assertThat(cached.get().getLongValue()).as("cached value of %s", key).contains(version);
        }
    }

    private static TsKvEntry entry(long version) {
        return new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("temperature", version), version);
    }

    private static ClusterNode pickMasterWithLiveReplica() {
        List<ClusterNode> nodes = clusterNodes();
        return nodes.stream()
                .filter(node -> node.master() && !node.failed())
                .filter(master -> nodes.stream().anyMatch(n -> master.id().equals(n.masterId()) && !n.failed()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No master with a live replica: " + nodes));
    }

    private static ClusterNode liveReplicaOf(ClusterNode master) {
        return clusterNodes().stream()
                .filter(node -> master.id().equals(node.masterId()) && !node.failed())
                .findFirst()
                .orElseThrow();
    }

    private static boolean isMaster(int port) {
        return clusterNodes(containerOf(port), port).stream().anyMatch(node -> node.port() == port && node.master());
    }

    /**
     * Every running node reports {@code cluster_state:ok}, and every running replica has finished syncing with its
     * master: Valkey does not promote a replica that never synced, so a failover test must wait for it.
     */
    private static boolean isClusterStable() {
        return runningPorts().stream().allMatch(port -> {
            GenericContainer<?> container = containerOf(port);
            String replication = exec(container, port, "INFO", "replication");
            return exec(container, port, "CLUSTER", "INFO").contains("cluster_state:ok")
                    && (replication.contains("role:master") || replication.contains("master_link_status:up"));
        });
    }

    private static List<ClusterNode> clusterNodes() {
        int port = runningPorts().get(0);
        return clusterNodes(containerOf(port), port);
    }

    private static List<ClusterNode> clusterNodes(GenericContainer<?> container, int port) {
        return exec(container, port, "CLUSTER", "NODES").lines().filter(line -> !line.isBlank()).map(ClusterNode::parse).toList();
    }

    private static List<Integer> runningPorts() {
        return PORTS.stream().filter(port -> containerOf(port).isRunning()).toList();
    }

    private static GenericContainer<?> containerOf(int port) {
        return CONTAINERS.get(PORTS.indexOf(port));
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

    /**
     * One line of {@code CLUSTER NODES}: {@code <id> <ip:port@cport> <flags> <master-id or -> ...}.
     */
    private record ClusterNode(String id, int port, boolean master, boolean failed, String masterId) {

        static ClusterNode parse(String line) {
            String[] fields = line.trim().split(" ");
            String address = fields[1];
            int port = Integer.parseInt(address.substring(address.indexOf(':') + 1, address.indexOf('@')));
            List<String> flags = Arrays.asList(fields[2].split(","));
            return new ClusterNode(fields[0], port, flags.contains("master"),
                    flags.contains("fail") || flags.contains("fail?"), fields[3]);
        }

    }

}
