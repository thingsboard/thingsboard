package org.thingsboard.server.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;


@Configuration
@ConditionalOnMissingBean(TbCaffeineCacheConfiguration.class)
@ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "sentinel")
public class TbRedisSentinelConfiguration extends TBRedisCacheConfiguration {

    @Value("${redis.username:}")
    private String username;

    @Value("${redis.password:}")
    private String password;

    @Value("${redis.sentinel.master:}")
    private String master;

    @Value("${redis.sentinel.sentinels:}")
    private String sentinels;

    @Value("${redis.cluster.useDefaultPoolConfig:true}")
    private boolean useDefaultPoolConfig;

    @Value("${redis.database:0}")
    private Integer database;

    @Value("${redis.password:}")
    private String sentinelPassword;

    public JedisConnectionFactory loadFactory() {
        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration();
        redisSentinelConfiguration.setSentinels(getNodes(sentinels));
        redisSentinelConfiguration.setMaster(this.master);
        redisSentinelConfiguration.setDatabase(this.database);
        redisSentinelConfiguration.setSentinelPassword(sentinelPassword);
        redisSentinelConfiguration.setUsername(username);
        redisSentinelConfiguration.setPassword(password);

        if (useDefaultPoolConfig) {
            return new JedisConnectionFactory(redisSentinelConfiguration);
        } else {
            return new JedisConnectionFactory(redisSentinelConfiguration, buildPoolConfig());
        }
    }

}
