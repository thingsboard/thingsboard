/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.discovery;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ZkPartitionDiscoveryService implements PartitionDiscoveryService, PathChildrenCacheListener {

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

    @Value("${queue.core.partitions:100}")
    private Integer corePartitions;
    @Value("${queue.rule_engine.partitions:100}")
    private Integer ruleEnginePartitions;

    @Autowired
    private TbServiceInfoProvider serviceIdProvider;

    private final ConcurrentMap<ServiceType, Integer> partitionSizes = new ConcurrentHashMap<>();
    private final ConcurrentMap<ServiceType, List<Integer>> myPartitions = new ConcurrentHashMap<>();

    private ExecutorService reconnectExecutorService;
    private CuratorFramework client;
    private PathChildrenCache cache;
    private String nodePath;
    private String zkNodesDir;

    private volatile boolean stopped = true;

    @Override
    public List<TopicPartitionInfo> getCurrentPartitions(ServiceType serviceType) {
        return Collections.emptyList();
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {

    }

    @PostConstruct
    public void init() {
        log.info("Initializing...");
        Assert.hasLength(zkUrl, missingProperty("zk.url"));
        Assert.notNull(zkRetryInterval, missingProperty("zk.retry_interval_ms"));
        Assert.notNull(zkConnectionTimeout, missingProperty("zk.connection_timeout_ms"));
        Assert.notNull(zkSessionTimeout, missingProperty("zk.session_timeout_ms"));

        reconnectExecutorService = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("zk-discovery"));

        partitionSizes.put(ServiceType.TB_CORE, corePartitions);
        partitionSizes.put(ServiceType.TB_RULE_ENGINE, ruleEnginePartitions);

        log.info("Initializing discovery service using ZK connect string: {}", zkUrl);

        zkNodesDir = zkDir + "/nodes";
        initZkClient();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (stopped) {
            log.debug("Ignoring application ready event. Service is stopped.");
            return;
        } else {
            log.info("Received application ready event. Starting current ZK node.");
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

    private void unpublishCurrentServer() {
        try {
            if (nodePath != null) {
                client.delete().forPath(nodePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete ZK node {}", nodePath, e);
            throw new RuntimeException(e);
        }
    }

    private void destroyZkClient() {
        stopped = true;
        try {
            unpublishCurrentServer();
        } catch (Exception e) {
        }
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

    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    @Override
    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {

    }
}
