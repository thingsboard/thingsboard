/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;

@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ZkDiscoveryService implements DiscoveryService, PathChildrenCacheListener {

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
    private PathChildrenCache cache;
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
        return cache.getCurrentData().stream()
                .filter(cd -> !cd.getPath().equals(nodePath))
                .map(cd -> {
                    try {
                        return TransportProtos.ServiceInfo.parseFrom(cd.getData());
                    } catch (NoSuchElementException | InvalidProtocolBufferException e) {
                        log.error("Failed to decode ZK node", e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

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
        if (currentServerExists()) {
            log.trace("[{}] Updating ZK node for current instance: {}", self.getServiceId(), nodePath);
            client.setData().forPath(nodePath, serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo().toByteArray());
        } else {
            try {
                log.info("[{}] Creating ZK node for current instance", self.getServiceId());
                nodePath = client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(zkNodesDir + "/", self.toByteArray());
                log.info("[{}] Created ZK node for current instance: {}", self.getServiceId(), nodePath);
                client.getConnectionStateListenable().addListener(checkReconnect(self));
            } catch (Exception e) {
                log.error("Failed to create ZK node", e);
                throw new RuntimeException(e);
            }
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

    private boolean currentServerExists() {
        if (nodePath == null) {
            return false;
        }
        try {
            TransportProtos.ServiceInfo self = serviceInfoProvider.getServiceInfo();
            TransportProtos.ServiceInfo registeredServerInfo = null;
            registeredServerInfo = TransportProtos.ServiceInfo.parseFrom(client.getData().forPath(nodePath));
            if (self.equals(registeredServerInfo)) {
                return true;
            }
        } catch (KeeperException.NoNodeException e) {
            log.info("ZK node does not exist: {}", nodePath);
        } catch (Exception e) {
            log.error("Couldn't check if ZK node exists", e);
        }
        return false;
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
            cache = new PathChildrenCache(client, zkNodesDir, true);
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
        cache.getListenable().addListener(this);
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

    @Override
    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
        if (stopped) {
            log.debug("Ignoring {}. Service is stopped.", pathChildrenCacheEvent);
            return;
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.debug("Ignoring {}, ZK client is not started, ZK client state [{}]", pathChildrenCacheEvent, client.getState());
            return;
        }
        ChildData data = pathChildrenCacheEvent.getData();
        if (data == null) {
            log.debug("Ignoring {} due to empty child data", pathChildrenCacheEvent);
            return;
        } else if (data.getData() == null) {
            log.debug("Ignoring {} due to empty child's data", pathChildrenCacheEvent);
            return;
        } else if (nodePath != null && nodePath.equals(data.getPath())) {
            if (pathChildrenCacheEvent.getType() == CHILD_REMOVED) {
                log.info("ZK node for current instance is somehow deleted.");
                publishCurrentServer();
            }
            log.debug("Ignoring event about current server {}", pathChildrenCacheEvent);
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

        log.trace("Processing [{}] event for [{}]", pathChildrenCacheEvent.getType(), serviceId);
        switch (pathChildrenCacheEvent.getType()) {
            case CHILD_ADDED:
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
            case CHILD_REMOVED:
                zkExecutorService.submit(() -> applicationEventPublisher.publishEvent(new OtherServiceShutdownEvent(this, serviceId, serviceTypesList)));
                ScheduledFuture<?> future = zkExecutorService.schedule(() -> {
                    log.debug("[{}] Going to recalculate partitions due to removed node [{}]",
                            serviceId, serviceTypesList);
                    ScheduledFuture<?> removedTask = delayedTasks.remove(serviceId);
                    if (removedTask != null) {
                        recalculatePartitions();
                    }
                }, recalculateDelay, TimeUnit.MILLISECONDS);
                delayedTasks.put(serviceId, future);
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
