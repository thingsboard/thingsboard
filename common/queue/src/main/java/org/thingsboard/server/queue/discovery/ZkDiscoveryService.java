// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.OtherServiceShutdownEvent;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.curator.framework.recipes.cache.CuratorCacheAccessor.parentPathFilter;

@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ZkDiscoveryService implements DiscoveryService {

    @Value("${zk.url}")
    private String zkUrl;
    @Value("${zk.retry_interval_ms}")
    private Integer zkRetryInterval;
    @Value("${zk.connection_timeout_ms}")
    private Integer zkConnectionTimeout;
    @Value("${zk.session_timeout_ms}")
    private Integer zkSessionTimeout;
    @Getter
    @Value("${zk.zk_dir}")
    private String zkDir;
    @Value("${zk.recalculate_delay:0}")
    private Long recalculateDelay;

    protected final ConcurrentHashMap<String, ScheduledFuture<?>> delayedTasks;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    private ScheduledExecutorService zkExecutorService;
    @Getter
    private CuratorFramework client;
    private CuratorCache cache;
    private String nodePath;
    private String zkNodesDir;

    private volatile boolean stopped = true;

    public ZkDiscoveryService(ApplicationEventPublisher applicationEventPublisher,
                              TbServiceInfoProvider serviceInfoProvider,
                              PartitionService partitionService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        delayedTasks = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        log.info("Initializing...");
        Assert.hasLength(zkUrl, missingProperty("zk.url"));
        Assert.notNull(zkRetryInterval, missingProperty("zk.retry_interval_ms"));
        Assert.notNull(zkConnectionTimeout, missingProperty("zk.connection_timeout_ms"));
        Assert.notNull(zkSessionTimeout, missingProperty("zk.session_timeout_ms"));

        zkExecutorService = ThingsBoardExecutors.newSingleThreadScheduledExecutor("zk-discovery");

        log.info("Initializing discovery service using ZK connect string: {}", zkUrl);

        zkNodesDir = zkDir + "/nodes";
        initZkClient();
    }

    @Override
    public List<TransportProtos.ServiceInfo> getOtherServers() {
        String currentServiceId = serviceInfoProvider.getServiceInfo().getServiceId();
        return cache.stream()
                .filter(parentPathFilter(zkNodesDir))
                .filter(cd -> !cd.getPath().equals(nodePath))
                .map(cd -> {
                    try {
                        return new Registration(cd, TransportProtos.ServiceInfo.parseFrom(cd.getData()));
                    } catch (NoSuchElementException | InvalidProtocolBufferException e) {
                        log.error("Failed to decode ZK node", e);
                        throw new RuntimeException(e);
                    }
                })
                .filter(r -> !r.info().getServiceId().equals(currentServiceId))
                .collect(Collectors.toMap(r -> r.info().getServiceId(), Function.identity(),
                        (a, b) -> b.data().getStat().getMtime() > a.data().getStat().getMtime() ? b : a,
                        LinkedHashMap::new))
                .values().stream()
                .map(Registration::info)
                .collect(Collectors.toList());
    }

    private record Registration(ChildData data, TransportProtos.ServiceInfo info) {}

    @Override
    public boolean isMonolith() {
        return false;
    }

    @AfterStartUp(order = AfterStartUp.DISCOVERY_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (stopped) {
            log.debug("Ignoring application ready event. Service is stopped.");
            return;
        } else {
            log.info("Received application ready event. Starting current ZK node.");
        }
        subscribeToEvents();
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.debug("Ignoring application ready event, ZK client is not started, ZK client state [{}]", client.getState());
            return;
        }
        log.info("Going to publish current server...");
        publishCurrentServer();
        log.info("Going to recalculate partitions...");
        recalculatePartitions();

        zkExecutorService.scheduleAtFixedRate(this::publishCurrentServer, 1, 1, TimeUnit.MINUTES);
    }

    @SneakyThrows
    public synchronized void publishCurrentServer() {
        TransportProtos.ServiceInfo self = serviceInfoProvider.getServiceInfo();
        switch (getCurrentServerNodeState()) {
            case PRESENT -> {
                log.trace("[{}] Updating ZK node for current instance: {}", self.getServiceId(), nodePath);
                client.setData().forPath(nodePath, serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo().toByteArray());
            }
            case ABSENT -> {
                try {
                    log.info("[{}] Creating ZK node for current instance", self.getServiceId());
                    nodePath = client.create()
                            .creatingParentsIfNeeded()
                            .withProtection()
                            .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(zkNodesDir + "/node-", self.toByteArray());
                    log.info("[{}] Created ZK node for current instance: {}", self.getServiceId(), nodePath);
                    client.getConnectionStateListenable().addListener(checkReconnect(self));
                } catch (Exception e) {
                    log.error("Failed to create ZK node", e);
                    throw new RuntimeException(e);
                }
            }
            case UNKNOWN -> log.debug("[{}] Skipping ZK node publish, node state is unknown", self.getServiceId());
        }
    }

    @Override
    public void setReady(boolean ready) {
        log.debug("Marking current service as {}", ready ? "ready" : "NOT ready");
        boolean changed = serviceInfoProvider.setReady(ready);
        if (changed) {
            try {
                publishCurrentServer();
            } catch (Exception e) {
                log.error("Failed to update server readiness status", e);
            }
        }
    }

    private NodeState getCurrentServerNodeState() {
        if (nodePath == null) {
            return NodeState.ABSENT;
        }
        try {
            Stat stat = client.checkExists().forPath(nodePath);
            if (stat == null) {
                log.info("ZK node does not exist: {}", nodePath);
                return NodeState.ABSENT;
            }
            long sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
            if (stat.getEphemeralOwner() != sessionId) {
                log.warn("ZK node {} is owned by another session 0x{}, current session 0x{}",
                        nodePath, Long.toHexString(stat.getEphemeralOwner()), Long.toHexString(sessionId));
                return NodeState.ABSENT;
            }
            return NodeState.PRESENT;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while checking if ZK node exists", e);
            return NodeState.UNKNOWN;
        } catch (Exception e) {
            log.error("Couldn't check if ZK node exists", e);
            return NodeState.UNKNOWN;
        }
    }

    private enum NodeState {
        PRESENT, ABSENT, UNKNOWN
    }

    private ConnectionStateListener checkReconnect(TransportProtos.ServiceInfo self) {
        return (client, newState) -> {
            log.info("[{}] ZK state changed: {}", self.getServiceId(), newState);
            if (newState == ConnectionState.LOST) {
                zkExecutorService.submit(this::reconnect);
            }
        };
    }

    private volatile boolean reconnectInProgress = false;

    private synchronized void reconnect() {
        if (!reconnectInProgress) {
            reconnectInProgress = true;
            try {
                destroyZkClient();
                initZkClient();
                subscribeToEvents();
                publishCurrentServer();
            } catch (Exception e) {
                log.error("Failed to reconnect to ZK: {}", e.getMessage(), e);
            } finally {
                reconnectInProgress = false;
            }
        }
    }

    private void initZkClient() {
        try {
            client = CuratorFrameworkFactory.newClient(zkUrl, zkSessionTimeout, zkConnectionTimeout, new RetryForever(zkRetryInterval));
            client.start();
            client.blockUntilConnected();
            client.createContainers(zkNodesDir);
            cache = CuratorCache.builder(client, zkNodesDir).build();
            cache.start();
            stopped = false;
            log.info("ZK client connected");
        } catch (Exception e) {
            log.error("Failed to connect to ZK: {}", e.getMessage(), e);
            CloseableUtils.closeQuietly(cache);
            CloseableUtils.closeQuietly(client);
            throw new RuntimeException(e);
        }
    }

    private void subscribeToEvents() {
        cache.listenable().addListener(this::onCacheEvent);
    }

    private void unpublishCurrentServer() {
        try {
            if (nodePath != null) {
                client.delete().forPath(nodePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete ZK node {}", nodePath, e);
        }
    }

    private void destroyZkClient() {
        stopped = true;
        unpublishCurrentServer();
        CloseableUtils.closeQuietly(cache);
        CloseableUtils.closeQuietly(client);
        log.info("ZK client disconnected");
    }

    @PreDestroy
    private void destroy() {
        zkExecutorService.shutdownNow();
        destroyZkClient();
        log.info("Stopped discovery service");
    }

    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    @SneakyThrows
    void onCacheEvent(CuratorCacheListener.Type type, ChildData oldData, ChildData newData) {
        if (stopped) {
            log.debug("Ignoring {}. Service is stopped.", type);
            return;
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.debug("Ignoring {}, ZK client is not started, ZK client state [{}]", type, client.getState());
            return;
        }
        ChildData data = type == CuratorCacheListener.Type.NODE_DELETED ? oldData : newData;
        if (data == null) {
            log.debug("Ignoring {} due to empty child data", type);
            return;
        } else if (data.getData() == null) {
            log.debug("Ignoring {} due to empty child's data", type);
            return;
        } else if (zkNodesDir.equals(data.getPath())) {
            log.debug("Ignoring event about parent node {}", data.getPath());
            return;
        } else if (nodePath != null && nodePath.equals(data.getPath())) {
            if (type == CuratorCacheListener.Type.NODE_DELETED) {
                log.info("ZK node for current instance is somehow deleted.");
                publishCurrentServer();
            }
            log.debug("Ignoring event about current server {}", data.getPath());
            return;
        }
        TransportProtos.ServiceInfo instance;
        try {
            instance = TransportProtos.ServiceInfo.parseFrom(data.getData());
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to decode server instance for node {}", data.getPath(), e);
            throw e;
        }

        String serviceId = instance.getServiceId();
        ProtocolStringList serviceTypesList = instance.getServiceTypesList();

        if (serviceId.equals(serviceInfoProvider.getServiceInfo().getServiceId())) {
            if (type == CuratorCacheListener.Type.NODE_CREATED) {
                log.error("[{}] Ignoring ZK node {} registered with the current service id. " +
                        "It is either a stale registration of this service or another service uses the same service id", serviceId, data.getPath());
            }
            return;
        }

        log.trace("Processing [{}] event for [{}]", type, serviceId);
        switch (type) {
            case NODE_CREATED:
                ScheduledFuture<?> task = delayedTasks.remove(serviceId);
                if (task != null) {
                    if (task.cancel(false)) {
                        log.info("[{}] Recalculate partitions ignored. Service was restarted in time [{}].",
                                serviceId, serviceTypesList);
                    } else {
                        log.debug("[{}] Going to recalculate partitions. Service was not restarted in time [{}]!",
                                serviceId, serviceTypesList);
                        recalculatePartitions();
                    }
                } else {
                    log.trace("[{}] Going to recalculate partitions due to adding new node [{}].",
                            serviceId, serviceTypesList);
                    recalculatePartitions();
                }
                break;
            case NODE_DELETED:
                if (getOtherServers().stream().anyMatch(s -> s.getServiceId().equals(serviceId))) {
                    log.debug("[{}] Ignoring removal of duplicate ZK node {}, service is still registered", serviceId, data.getPath());
                    break;
                }
                zkExecutorService.submit(() -> applicationEventPublisher.publishEvent(new OtherServiceShutdownEvent(this, serviceId, serviceTypesList)));
                delayedTasks.compute(serviceId, (id, previous) -> zkExecutorService.schedule(() -> {
                    log.debug("[{}] Going to recalculate partitions due to removed node [{}]",
                            serviceId, serviceTypesList);
                    ScheduledFuture<?> removedTask = delayedTasks.remove(serviceId);
                    if (removedTask != null) {
                        recalculatePartitions();
                    }
                }, recalculateDelay, TimeUnit.MILLISECONDS));
                break;
            default:
                break;
        }
    }

    /**
     * A single entry point to recalculate partitions
     * Synchronized to ensure that other servers info is up to date
     * */
    synchronized void recalculatePartitions() {
        delayedTasks.values().forEach(future -> future.cancel(false));
        delayedTasks.clear();
        partitionService.recalculatePartitions(serviceInfoProvider.getServiceInfo(), getOtherServers());
    }

}
