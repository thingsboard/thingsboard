/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
