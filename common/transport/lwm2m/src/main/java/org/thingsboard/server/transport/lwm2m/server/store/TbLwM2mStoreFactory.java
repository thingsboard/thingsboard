// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;

import java.util.Optional;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2mStoreFactory {

    private final Optional<TBRedisCacheConfiguration> redisConfiguration;
    private final LwM2MTransportServerConfig config;
    private final LwM2mCredentialsSecurityInfoValidator validator;
    private final LwM2mVersionedModelProvider modelProvider;

    @Bean
    private RegistrationStore registrationStore() {
        return redisConfiguration.isPresent() ?
                new TbLwM2mRedisRegistrationStore(config, getConnectionFactory(), modelProvider) :
                new TbInMemoryRegistrationStore(config, config.getCleanPeriodInSec(), modelProvider);
    }

    @Bean
    private TbMainSecurityStore securityStore() {
        return new TbLwM2mSecurityStore(redisConfiguration.isPresent() ?
                new TbLwM2mRedisSecurityStore(getConnectionFactory()) : new TbInMemorySecurityStore(), validator);
    }

    @Bean
    private TbLwM2MClientStore clientStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MClientStore(getConnectionFactory()) : new TbDummyLwM2MClientStore();
    }

    @Bean
    private TbLwM2MModelConfigStore modelConfigStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MModelConfigStore(getConnectionFactory()) : new TbDummyLwM2MModelConfigStore();
    }

    @Bean
    private TbLwM2MClientOtaInfoStore otaStore() {
        return redisConfiguration.isPresent() ? new TbLwM2mRedisClientOtaInfoStore(getConnectionFactory()) : new TbDummyLwM2MClientOtaInfoStore();
    }

    @Bean
    private TbLwM2MDtlsSessionStore sessionStore() {
        return redisConfiguration.isPresent() ? new TbLwM2MDtlsSessionRedisStore(getConnectionFactory()) : new TbL2M2MDtlsSessionInMemoryStore();
    }

    private RedisConnectionFactory getConnectionFactory() {
        return redisConfiguration.get().redisConnectionFactory();
    }

}
