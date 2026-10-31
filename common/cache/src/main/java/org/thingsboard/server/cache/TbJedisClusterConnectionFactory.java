// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.thingsboard.server.common.data.StringUtils;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cluster connection factory that creates a {@link TbJedisCluster}, so the cache can renew the Jedis slot map after
 * a topology change, and the slot map is also refreshed in the background.
 */
public class TbJedisClusterConnectionFactory extends JedisConnectionFactory {

    private static final int DEFAULT_MAX_REDIRECTS = 5; // same default as JedisConnectionFactory#createCluster

    private final Duration topologyRefreshPeriod;

    /**
     * @param topologyRefreshPeriod period of the background slot map refresh, or {@code null} to disable it
     */
    public TbJedisClusterConnectionFactory(RedisClusterConfiguration clusterConfiguration, JedisClientConfiguration clientConfiguration,
                                           Duration topologyRefreshPeriod) {
        super(clusterConfiguration, clientConfiguration);
        this.topologyRefreshPeriod = topologyRefreshPeriod;
    }

    @Override
    protected JedisCluster createCluster(RedisClusterConfiguration clusterConfig, GenericObjectPoolConfig<Connection> poolConfig) {
        Set<HostAndPort> nodes = clusterConfig.getClusterNodes().stream()
                .map(node -> new HostAndPort(node.getHost(), node.getPort()))
                .collect(Collectors.toSet());
        int maxRedirects = clusterConfig.getMaxRedirects() != null ? clusterConfig.getMaxRedirects() : DEFAULT_MAX_REDIRECTS;
        return new TbJedisCluster(nodes, createClusterClientConfig(clusterConfig), poolConfig, topologyRefreshPeriod, maxRedirects);
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
        clientConfiguration.getCustomizer().ifPresent(customizer -> customizer.customize(builder));
        return builder.build();
    }

}
