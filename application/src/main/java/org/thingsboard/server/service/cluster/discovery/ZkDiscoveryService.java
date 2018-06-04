/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.utils.MiscUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ZkDiscoveryService implements DiscoveryService, PathChildrenCacheListener, ApplicationListener<ApplicationReadyEvent> {

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

    private final List<DiscoveryServiceListener> listeners = new CopyOnWriteArrayList<>();

    private CuratorFramework client;
    private PathChildrenCache cache;
    private String nodePath;


    @PostConstruct
    public void init() {
        log.info("Initializing...");
        Assert.hasLength(zkUrl, MiscUtils.missingProperty("zk.url"));
        Assert.notNull(zkRetryInterval, MiscUtils.missingProperty("zk.retry_interval_ms"));
        Assert.notNull(zkConnectionTimeout, MiscUtils.missingProperty("zk.connection_timeout_ms"));
        Assert.notNull(zkSessionTimeout, MiscUtils.missingProperty("zk.session_timeout_ms"));

        log.info("Initializing discovery service using ZK connect string: {}", zkUrl);

        zkNodesDir = zkDir + "/nodes";
        try {
            client = CuratorFrameworkFactory.newClient(zkUrl, zkSessionTimeout, zkConnectionTimeout, new RetryForever(zkRetryInterval));
            client.start();
            client.blockUntilConnected();
            cache = new PathChildrenCache(client, zkNodesDir, true);
            cache.getListenable().addListener(this);
            cache.start();
        } catch (Exception e) {
            log.error("Failed to connect to ZK: {}", e.getMessage(), e);
            CloseableUtils.closeQuietly(client);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        unpublishCurrentServer();
        CloseableUtils.closeQuietly(client);
        log.info("Stopped discovery service");
    }

    @Override
    public void publishCurrentServer() {
        try {
            ServerInstance self = this.serverInstance.getSelf();
            log.info("[{}:{}] Creating ZK node for current instance", self.getHost(), self.getPort());
            nodePath = client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(zkNodesDir + "/", SerializationUtils.serialize(self.getServerAddress()));
            log.info("[{}:{}] Created ZK node for current instance: {}", self.getHost(), self.getPort(), nodePath);
        } catch (Exception e) {
            log.error("Failed to create ZK node", e);
            throw new RuntimeException(e);
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
                        return new ServerInstance( (ServerAddress) SerializationUtils.deserialize(cd.getData()));
                    } catch (NoSuchElementException e) {
                        log.error("Failed to decode ZK node", e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean addListener(DiscoveryServiceListener listener) {
        return listeners.add(listener);
    }

    @Override
    public boolean removeListener(DiscoveryServiceListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        publishCurrentServer();
        getOtherServers().forEach(
                server -> log.info("Found active server: [{}:{}]", server.getHost(), server.getPort())
        );
    }

    @Override
    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
        ChildData data = pathChildrenCacheEvent.getData();
        if (data == null) {
            log.debug("Ignoring {} due to empty child data", pathChildrenCacheEvent);
            return;
        } else if (data.getData() == null) {
            log.debug("Ignoring {} due to empty child's data", pathChildrenCacheEvent);
            return;
        } else if (nodePath != null && nodePath.equals(data.getPath())) {
            log.debug("Ignoring event about current server {}", pathChildrenCacheEvent);
            return;
        }
        ServerInstance instance;
        try {
            ServerAddress serverAddress  = SerializationUtils.deserialize(data.getData());
            instance = new ServerInstance(serverAddress);
        } catch (SerializationException e) {
            log.error("Failed to decode server instance for node {}", data.getPath(), e);
            throw e;
        }
        log.info("Processing [{}] event for [{}:{}]", pathChildrenCacheEvent.getType(), instance.getHost(), instance.getPort());
        switch (pathChildrenCacheEvent.getType()) {
            case CHILD_ADDED:
                tsSubService.onClusterUpdate();
                deviceStateService.onClusterUpdate();
                listeners.forEach(listener -> listener.onServerAdded(instance));
                break;
            case CHILD_UPDATED:
                listeners.forEach(listener -> listener.onServerUpdated(instance));
                break;
            case CHILD_REMOVED:
                tsSubService.onClusterUpdate();
                deviceStateService.onClusterUpdate();
                listeners.forEach(listener -> listener.onServerRemoved(instance));
                break;
            default:
                break;
        }
    }
}
