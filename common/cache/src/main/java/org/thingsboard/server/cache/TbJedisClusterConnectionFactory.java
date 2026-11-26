// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.util.RedisClientLibraryInfo;
import org.thingsboard.server.common.data.StringUtils;
import redis.clients.jedis.ClientSetInfoConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DriverInfo;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cluster connection factory whose Jedis slot map can be renewed on demand and is also refreshed in the background.
 * <p>
 * Commands sent through the cluster command executor renew the slot map themselves, on MOVED and on repeated
 * connection errors. Commands that need one node connection (WATCH/MULTI, a script pinned to one slot) borrow it
 * through {@link RedisClusterClient#getConnectionFromSlot(int)}, which bypasses that executor. When such a command
 * fails because the cluster topology changed, the caller must call {@link #renewSlotCache()}. Otherwise every later
 * call goes to the old node again, and if that node's address is gone, it fails until the JVM restarts.
 */
public class TbJedisClusterConnectionFactory extends JedisConnectionFactory {

    private static final int DEFAULT_MAX_REDIRECTS = 5; // same default as JedisConnectionFactory#createRedisClusterClient

    private final Duration topologyRefreshPeriod;
    private volatile ClusterConnectionProvider connectionProvider;

    /**
     * @param topologyRefreshPeriod period of the background slot map refresh, or {@code null} to disable it
     */
    public TbJedisClusterConnectionFactory(RedisClusterConfiguration clusterConfiguration, JedisClientConfiguration clientConfiguration,
                                           Duration topologyRefreshPeriod) {
        super(clusterConfiguration, clientConfiguration);
        this.topologyRefreshPeriod = topologyRefreshPeriod;
    }

    @Override
    protected RedisClusterClient createRedisClusterClient(RedisClusterConfiguration clusterConfig) {
        Set<HostAndPort> nodes = clusterConfig.getClusterNodes().stream()
                .map(node -> new HostAndPort(node.getHost(), node.getPort()))
                .collect(Collectors.toSet());
        int maxRedirects = clusterConfig.getMaxRedirects() != null ? clusterConfig.getMaxRedirects() : DEFAULT_MAX_REDIRECTS;
        JedisClientConfig clientConfig = createClusterClientConfig(clusterConfig);
        GenericObjectPoolConfig<Connection> poolConfig = getPoolConfig();
        ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, clientConfig, poolConfig, topologyRefreshPeriod);

        RedisClusterClient.Builder builder = RedisClusterClient.builder();
        builder.nodes(nodes).clientConfig(clientConfig).maxAttempts(maxRedirects).poolConfig(poolConfig);
        getClientConfiguration().getClientCustomizer().ifPresent(customizer -> customizer.customize(builder));
        builder.connectionProvider(provider);
        RedisClusterClient client = builder.build();
        connectionProvider = provider;
        return client;
    }

    /**
     * Reloads the slot map from the cluster. Seed nodes are asked first, so this works even when every known node
     * address has changed. Returns immediately if another thread is already renewing it, or if the factory is not started.
     */
    public void renewSlotCache() {
        ClusterConnectionProvider provider = connectionProvider;
        if (provider != null) {
            provider.renewSlotCache();
        }
    }

    /**
     * Builds the same Jedis client config as {@code JedisConnectionFactory#createClientConfig}, which is private.
     */
    JedisClientConfig createClusterClientConfig(RedisClusterConfiguration clusterConfig) {
        JedisClientConfiguration clientConfiguration = getClientConfiguration();
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();
        clientConfiguration.getClientName().ifPresent(builder::clientName);
        builder.connectionTimeoutMillis(Math.toIntExact(clientConfiguration.getConnectTimeout().toMillis()));
        builder.socketTimeoutMillis(Math.toIntExact(clientConfiguration.getReadTimeout().toMillis()));
        builder.clientSetInfoConfig(new ClientSetInfoConfig(DriverInfo.builder()
                .addUpstreamDriver(RedisClientLibraryInfo.FRAMEWORK_NAME, RedisClientLibraryInfo.getVersion())
                .build()));
        builder.database(getDatabase());
        if (StringUtils.isNotEmpty(clusterConfig.getUsername())) {
            builder.user(clusterConfig.getUsername());
        }
        clusterConfig.getPassword().toOptional().map(String::new).ifPresent(builder::password);
        if (clientConfiguration.isUseSsl()) {
            builder.ssl(true);
            clientConfiguration.getSslSocketFactory().ifPresent(builder::sslSocketFactory);
            clientConfiguration.getHostnameVerifier().ifPresent(builder::hostnameVerifier);
            clientConfiguration.getSslParameters().ifPresent(builder::sslParameters);
        }
        clientConfiguration.getClientConfigCustomizer().ifPresent(customizer -> customizer.customize(builder));
        return builder.build();
    }

}
