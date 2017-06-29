/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.GroupProperty;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import com.hazelcast.zookeeper.ZookeeperDiscoveryProperties;
import com.hazelcast.zookeeper.ZookeeperDiscoveryStrategyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.common.data.CacheConstants;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "cache", value = "enabled", havingValue = "true")
public class ServiceCacheConfiguration {

    private static final String HAZELCAST_CLUSTER_NAME = "hazelcast";

    @Value("${cache.device_credentials.max_size.size}")
    private Integer cacheDeviceCredentialsMaxSizeSize;
    @Value("${cache.device_credentials.max_size.policy}")
    private String cacheDeviceCredentialsMaxSizePolicy;
    @Value("${cache.device_credentials.time_to_live}")
    private Integer cacheDeviceCredentialsTTL;

    @Value("${zk.enabled}")
    private boolean zkEnabled;
    @Value("${zk.url}")
    private String zkUrl;
    @Value("${zk.zk_dir}")
    private String zkDir;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();

        if (zkEnabled) {
            addZkConfig(config);
        }

        config.addMapConfig(createDeviceCredentialsCacheConfig());

        return Hazelcast.newHazelcastInstance(config);
    }

    private void addZkConfig(Config config) {
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.setProperty(GroupProperty.DISCOVERY_SPI_ENABLED.getName(), Boolean.TRUE.toString());
        DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(new ZookeeperDiscoveryStrategyFactory());
        discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(), zkUrl);
        discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_PATH.key(), zkDir);
        discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), HAZELCAST_CLUSTER_NAME);
        config.getNetworkConfig().getJoin().getDiscoveryConfig().addDiscoveryStrategyConfig(discoveryStrategyConfig);
    }

    private MapConfig createDeviceCredentialsCacheConfig() {
        MapConfig deviceCredentialsCacheConfig = new MapConfig(CacheConstants.DEVICE_CREDENTIALS_CACHE);
        deviceCredentialsCacheConfig.setTimeToLiveSeconds(cacheDeviceCredentialsTTL);
        deviceCredentialsCacheConfig.setEvictionPolicy(EvictionPolicy.LRU);
        deviceCredentialsCacheConfig.setMaxSizeConfig(
                new MaxSizeConfig(
                        cacheDeviceCredentialsMaxSizeSize,
                        MaxSizeConfig.MaxSizePolicy.valueOf(cacheDeviceCredentialsMaxSizePolicy))
        );
        return deviceCredentialsCacheConfig;
    }

    @Bean
    public KeyGenerator previousDeviceCredentialsId() {
        return new PreviousDeviceCredentialsIdKeyGenerator();
    }

    @Bean
    public CacheManager cacheManager() {
        return new HazelcastCacheManager(hazelcastInstance());
    }
}
