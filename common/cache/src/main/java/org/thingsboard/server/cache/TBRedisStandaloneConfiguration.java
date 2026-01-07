/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.time.Duration;

@Configuration
@ConditionalOnMissingBean(TbCaffeineCacheConfiguration.class)
@ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "standalone")
public class TBRedisStandaloneConfiguration extends TBRedisCacheConfiguration {

    @Value("${redis.standalone.host:localhost}")
    private String host;

    @Value("${redis.standalone.port:6379}")
    private Integer port;

    @Value("${redis.standalone.clientName:standalone}")
    private String clientName;

    @Value("${redis.standalone.connectTimeout:30000}")
    private Long connectTimeout;

    @Value("${redis.standalone.readTimeout:60000}")
    private Long readTimeout;

    @Value("${redis.standalone.useDefaultClientConfig:true}")
    private boolean useDefaultClientConfig;

    @Value("${redis.standalone.usePoolConfig:false}")
    private boolean usePoolConfig;

    @Value("${redis.db:0}")
    private Integer db;

    @Value("${redis.username:}")
    private String username;

    @Value("${redis.password:}")
    private String password;

    @Value("${redis.ssl.enabled:false}")
    private boolean useSsl;

    public JedisConnectionFactory loadFactory() {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(host);
        standaloneConfiguration.setPort(port);
        standaloneConfiguration.setDatabase(db);
        standaloneConfiguration.setUsername(username);
        standaloneConfiguration.setPassword(password);
        return new JedisConnectionFactory(standaloneConfiguration, buildClientConfig());
    }

    private JedisClientConfiguration buildClientConfig() {
        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfigurationBuilder = JedisClientConfiguration.builder();
        if (!useDefaultClientConfig) {
            jedisClientConfigurationBuilder
                    .clientName(clientName)
                    .connectTimeout(Duration.ofMillis(connectTimeout))
                    .readTimeout(Duration.ofMillis(readTimeout));
        }
        if (useSsl) {
            jedisClientConfigurationBuilder
                    .useSsl()
                    .sslSocketFactory(createSslSocketFactory());
        }
        if (usePoolConfig) {
            jedisClientConfigurationBuilder
                    .usePooling()
                    .poolConfig(buildPoolConfig());
        }
        return jedisClientConfigurationBuilder.build();
    }

}
