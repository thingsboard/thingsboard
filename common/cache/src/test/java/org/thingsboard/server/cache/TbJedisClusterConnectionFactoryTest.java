// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import redis.clients.jedis.JedisClientConfig;

import javax.net.ssl.SSLSocketFactory;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link TbJedisClusterConnectionFactory} builds the Jedis client config itself, because Spring keeps its own private.
 * It must carry every setting ThingsBoard passes to Spring, or cluster connections lose auth, TLS or timeouts.
 */
class TbJedisClusterConnectionFactoryTest {

    @Test
    void givenDefaultClientConfiguration_whenCreateClusterClientConfig_thenUsesSpringDefaults() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(List.of("127.0.0.1:6379"));
        var factory = new TbJedisClusterConnectionFactory(clusterConfig, JedisClientConfiguration.defaultConfiguration(), null);

        JedisClientConfig config = factory.createClusterClientConfig(clusterConfig);

        assertThat(config.getConnectionTimeoutMillis()).isEqualTo(2000);
        assertThat(config.getSocketTimeoutMillis()).isEqualTo(2000);
        assertThat(config.getDatabase()).isZero();
        assertThat(config.getUser()).isNull();
        assertThat(config.getPassword()).isNull();
        assertThat(config.isSsl()).isFalse();
    }

    @Test
    void givenAuthTlsAndTimeouts_whenCreateClusterClientConfig_thenAllAreApplied() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(List.of("127.0.0.1:6379"));
        clusterConfig.setUsername("tb");
        clusterConfig.setPassword("secret");
        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
        JedisClientConfiguration clientConfiguration = JedisClientConfiguration.builder()
                .clientName("tb-node")
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(4))
                .useSsl().sslSocketFactory(sslSocketFactory)
                .and().build();
        var factory = new TbJedisClusterConnectionFactory(clusterConfig, clientConfiguration, Duration.ofSeconds(30));

        JedisClientConfig config = factory.createClusterClientConfig(clusterConfig);

        assertThat(config.getClientName()).isEqualTo("tb-node");
        assertThat(config.getConnectionTimeoutMillis()).isEqualTo(3000);
        assertThat(config.getSocketTimeoutMillis()).isEqualTo(4000);
        assertThat(config.getUser()).isEqualTo("tb");
        assertThat(config.getPassword()).isEqualTo("secret");
        assertThat(config.isSsl()).isTrue();
        assertThat(config.getSslSocketFactory()).isSameAs(sslSocketFactory);
    }

}
