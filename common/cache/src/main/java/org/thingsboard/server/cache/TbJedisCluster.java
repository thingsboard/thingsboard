// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.time.Duration;
import java.util.Set;

/**
 * {@link JedisCluster} whose slot map can be renewed on demand.
 * <p>
 * Commands sent through the cluster command executor renew the slot map themselves, on MOVED and on repeated
 * connection errors. Commands that need one node connection (WATCH/MULTI, a script pinned to one slot) borrow it
 * through {@link #getConnectionFromSlot(int)}, which bypasses that executor. When such a command fails because the
 * cluster topology changed, the caller must call {@link #renewSlotCache()}. Otherwise every later call goes to the
 * old node again, and if that node's address is gone, it fails until the JVM restarts.
 */
public class TbJedisCluster extends JedisCluster {

    /**
     * @param topologyRefreshPeriod period of the background slot map refresh, or {@code null} to disable it
     */
    public TbJedisCluster(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig, GenericObjectPoolConfig<Connection> poolConfig,
                          Duration topologyRefreshPeriod, int maxAttempts) {
        // Same retry budget as the JedisCluster constructor used by Spring's JedisConnectionFactory.
        super(clusterNodes, clientConfig, poolConfig, topologyRefreshPeriod, maxAttempts,
                Duration.ofMillis((long) clientConfig.getSocketTimeoutMillis() * maxAttempts));
    }

    /**
     * Reloads the slot map from the cluster. Seed nodes are asked first, so this works even when every known node
     * address has changed. Returns immediately if another thread is already renewing it.
     */
    public void renewSlotCache() {
        ((ClusterConnectionProvider) provider).renewSlotCache();
    }

}
