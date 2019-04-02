/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.cluster.discovery;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.utils.MiscUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;

/**
 * @author Andrew Shvayka
 */
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
    @Value("${zk.zk_dir}")
    private String zkDir;

    private String zkNodesDir;

    @Autowired
    private ServerInstanceService serverInstance;

    @Autowired
    @Lazy
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    @Lazy
    private DeviceStateService deviceStateService;

    @Autowired
    @Lazy
    private ActorService actorService;

    @Autowired
    @Lazy
    private ClusterRoutingService routingService;

    private ExecutorService reconnectExecutorService;

    private CuratorFramework client;
    private PathChildrenCache cache;
    private String nodePath;

    private volatile boolean stopped = true;

    @PostConstruct
    public void init() {
        log.info("Initializing...");
        Assert.hasLength(zkUrl, MiscUtils.missingProperty("zk.url"));
        Assert.notNull(zkRetryInterval, MiscUtils.missingProperty("zk.retry_interval_ms"));
        Assert.notNull(zkConnectionTimeout, MiscUtils.missingProperty("zk.connection_timeout_ms"));
        Assert.notNull(zkSessionTimeout, MiscUtils.missingProperty("zk.session_timeout_ms"));

        reconnectExecutorService = Executors.newSingleThreadExecutor();

        log.info("Initializing discovery service using ZK connect string: {}", zkUrl);

        zkNodesDir = zkDir + "/nodes";
        initZkClient();
    }

    private void initZkClient() {
        try {
            client = CuratorFrameworkFactory.newClient(zkUrl, zkSessionTimeout, zkConnectionTimeout, new RetryForever(zkRetryInterval));
            client.start();
            client.blockUntilConnected();
            cache = new PathChildrenCache(client, zkNodesDir, true);
            cache.getListenable().addListener(this);
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

    private void destroyZkClient() {
        stopped = true;
        try {
            unpublishCurrentServer();
        } catch (Exception e) {}
        CloseableUtils.closeQuietly(cache);
        CloseableUtils.closeQuietly(client);
        log.info("ZK client disconnected");
    }

    @PreDestroy
    public void destroy() {
        destroyZkClient();
        reconnectExecutorService.shutdownNow();
        log.info("Stopped discovery service");
    }

    @Override
    public synchronized void publishCurrentServer() {
        ServerInstance self = this.serverInstance.getSelf();
        if (currentServerExists()) {
            log.info("[{}:{}] ZK node for current instance already exists, NOT created new one: {}", self.getHost(), self.getPort(), nodePath);
        } else {
            try {
                log.info("[{}:{}] Creating ZK node for current instance", self.getHost(), self.getPort());
                nodePath = client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(zkNodesDir + "/", SerializationUtils.serialize(self.getServerAddress()));
                log.info("[{}:{}] Created ZK node for current instance: {}", self.getHost(), self.getPort(), nodePath);
                client.getConnectionStateListenable().addListener(checkReconnect(self));
            } catch (Exception e) {
                log.error("Failed to create ZK node", e);
                throw new RuntimeException(e);
            }
        }
    }

    private boolean currentServerExists() {
        if (nodePath == null) {
            return false;
        }
        try {
            ServerInstance self = this.serverInstance.getSelf();
            ServerAddress registeredServerAdress = null;
            registeredServerAdress = SerializationUtils.deserialize(client.getData().forPath(nodePath));
            if (self.getServerAddress() != null && self.getServerAddress().equals(registeredServerAdress)) {
                return true;
            }
        } catch (KeeperException.NoNodeException e) {
            log.info("ZK node does not exist: {}", nodePath);
        } catch (Exception e) {
            log.error("Couldn't check if ZK node exists", e);
        }
        return false;
    }

    private ConnectionStateListener checkReconnect(ServerInstance self) {
        return (client, newState) -> {
            log.info("[{}:{}] ZK state changed: {}", self.getHost(), self.getPort(), newState);
            if (newState == ConnectionState.LOST) {
                reconnectExecutorService.submit(this::reconnect);
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
                publishCurrentServer();
            } catch (Exception e) {
                log.error("Failed to reconnect to ZK: {}", e.getMessage(), e);
            } finally {
                reconnectInProgress = false;
            }
        }
    }

    @Override
    public void unpublishCurrentServer() {
        try {
            if (nodePath != null) {
                client.delete().forPath(nodePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete ZK node {}", nodePath, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServerInstance getCurrentServer() {
        return serverInstance.getSelf();
    }

    @Override
    public List<ServerInstance> getOtherServers() {
        return cache.getCurrentData().stream()
                .filter(cd -> !cd.getPath().equals(nodePath))
                .map(cd -> {
                    try {
                        return new ServerInstance((ServerAddress) SerializationUtils.deserialize(cd.getData()));
                    } catch (NoSuchElementException e) {
                        log.error("Failed to decode ZK node", e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting current ZK node.");
        if (stopped) {
            log.debug("Ignoring application ready event. Service is stopped.");
            return;
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.debug("Ignoring application ready event, ZK client is not started, ZK client state [{}]", client.getState());
            return;
        }
        publishCurrentServer();
        getOtherServers().forEach(
                server -> log.info("Found active server: [{}:{}]", server.getHost(), server.getPort())
        );
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
        ServerInstance instance;
        try {
            ServerAddress serverAddress = SerializationUtils.deserialize(data.getData());
            instance = new ServerInstance(serverAddress);
        } catch (SerializationException e) {
            log.error("Failed to decode server instance for node {}", data.getPath(), e);
            throw e;
        }
        log.info("Processing [{}] event for [{}:{}]", pathChildrenCacheEvent.getType(), instance.getHost(), instance.getPort());
        switch (pathChildrenCacheEvent.getType()) {
            case CHILD_ADDED:
                routingService.onServerAdded(instance);
                tsSubService.onClusterUpdate();
                deviceStateService.onClusterUpdate();
                actorService.onServerAdded(instance);
                break;
            case CHILD_UPDATED:
                routingService.onServerUpdated(instance);
                actorService.onServerUpdated(instance);
                break;
            case CHILD_REMOVED:
                routingService.onServerRemoved(instance);
                tsSubService.onClusterUpdate();
                deviceStateService.onClusterUpdate();
                actorService.onServerRemoved(instance);
                break;
            default:
                break;
        }
    }
}
