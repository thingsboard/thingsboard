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
package org.thingsboard.server.dao.cache;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis", matchIfMissing = false)
@EnableCaching
@Data
public class TBRedisCacheConfiguration {


    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String STANDALONE_TYPE = "standalone";
    private static final String CLUSTER_TYPE = "cluster";

    @Value("${redis.connection.type}")
    private String connectionType;

    @Value("${redis.standalone.host}")
    private String host;

    @Value("${redis.standalone.port}")
    private Integer port;

    @Value("${redis.cluster.nodes}")
    private String clusterNodes;

    @Value("${redis.cluster.max-redirects}")
    private Integer maxRedirects;

    @Value("${redis.db}")
    private Integer db;

    @Value("${redis.password}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (connectionType.equalsIgnoreCase(STANDALONE_TYPE)) {
            RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
            standaloneConfiguration.setHostName(host);
            standaloneConfiguration.setPort(port);
            standaloneConfiguration.setDatabase(db);
            standaloneConfiguration.setPassword(password);
            return new JedisConnectionFactory(standaloneConfiguration);
        } else if (connectionType.equalsIgnoreCase(CLUSTER_TYPE)) {
            RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
            clusterConfiguration.setClusterNodes(getNodes(clusterNodes));
            clusterConfiguration.setMaxRedirects(maxRedirects);
            clusterConfiguration.setPassword(password);
            return new JedisConnectionFactory(clusterConfiguration);
        } else {
            throw new RuntimeException("Unsupported Redis Connection type: [" + connectionType + "]!");
        }
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf) {
        DefaultFormattingConversionService redisConversionService = new DefaultFormattingConversionService();
        RedisCacheConfiguration.registerDefaultConverters(redisConversionService);
        registerDefaultConverters(redisConversionService);
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig().withConversionService(redisConversionService);
        return RedisCacheManager.builder(cf).cacheDefaults(configuration).build();
    }

    @Bean
    public KeyGenerator previousDeviceCredentialsId() {
        return new PreviousDeviceCredentialsIdKeyGenerator();
    }

    private List<RedisNode> getNodes(String nodes) {
        List<RedisNode> result;
        if (StringUtils.isBlank(nodes)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (String hostPort : nodes.split(COMMA)) {
                String host = hostPort.split(COLON)[0];
                Integer port = Integer.valueOf(hostPort.split(COLON)[1]);
                result.add(new RedisNode(host, port));
            }
        }
        return result;
    }

    private static void registerDefaultConverters(ConverterRegistry registry) {
        Assert.notNull(registry, "ConverterRegistry must not be null!");
        registry.addConverter(EntityId.class, String.class, EntityId::toString);
    }
}
