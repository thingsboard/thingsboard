/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.id.EntityId;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis", matchIfMissing = false)
@EnableCaching
@Data
public abstract class TBRedisCacheConfiguration {

    @Value("${redis.pool_config.maxTotal}")
    private int maxTotal;

    @Value("${redis.pool_config.maxIdle}")
    private int maxIdle;

    @Value("${redis.pool_config.minIdle}")
    private int minIdle;

    @Value("${redis.pool_config.testOnBorrow}")
    private boolean testOnBorrow;

    @Value("${redis.pool_config.testOnReturn}")
    private boolean testOnReturn;

    @Value("${redis.pool_config.testWhileIdle}")
    private boolean testWhileIdle;

    @Value("${redis.pool_config.minEvictableMs}")
    private long minEvictableMs;

    @Value("${redis.pool_config.evictionRunsMs}")
    private long evictionRunsMs;

    @Value("${redis.pool_config.maxWaitMills}")
    private long maxWaitMills;

    @Value("${redis.pool_config.numberTestsPerEvictionRun}")
    private int numberTestsPerEvictionRun;

    @Value("${redis.pool_config.blockWhenExhausted}")
    private boolean blockWhenExhausted;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return loadFactory();
    }

    protected abstract JedisConnectionFactory loadFactory();

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

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    private static void registerDefaultConverters(ConverterRegistry registry) {
        Assert.notNull(registry, "ConverterRegistry must not be null!");
        registry.addConverter(EntityId.class, String.class, EntityId::toString);
    }

    protected JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(testOnBorrow);
        poolConfig.setTestOnReturn(testOnReturn);
        poolConfig.setTestWhileIdle(testWhileIdle);
        poolConfig.setMinEvictableIdleTimeMillis(minEvictableMs);
        poolConfig.setTimeBetweenEvictionRunsMillis(evictionRunsMs);
        poolConfig.setMaxWaitMillis(maxWaitMills);
        poolConfig.setNumTestsPerEvictionRun(numberTestsPerEvictionRun);
        poolConfig.setBlockWhenExhausted(blockWhenExhausted);
        return poolConfig;
    }
}
