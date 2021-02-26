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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.Collection;
import java.util.Optional;

@Service
@TbLwM2mTransportComponent
public class TbLwM2mStoreConfiguration {

    @Autowired(required = false)
    private Optional<TBRedisCacheConfiguration> redisConfiguration;

    @Autowired
    @Lazy
    private LwM2mClientContext clientContext;

    @Value("${transport.lwm2m.redis.enabled:false}")
    private boolean useRedis;

    @Bean
    private CaliforniumRegistrationStore registrationStore() {
        return redisConfiguration.isPresent() && useRedis ?
                new TbLwM2mRedisRegistrationStore(redisConfiguration.get().redisConnectionFactory()) : new InMemoryRegistrationStore();
    }

    @Bean
    private EditableSecurityStore securityStore() {
        return new TbLwM2mSecurityStoreWrapper(redisConfiguration.isPresent() && useRedis ?
                new TbLwM2mRedisSecurityStore(redisConfiguration.get().redisConnectionFactory()) : new InMemorySecurityStore());
    }

    public class TbLwM2mSecurityStoreWrapper implements EditableSecurityStore {

        private final EditableSecurityStore securityStore;

        public TbLwM2mSecurityStoreWrapper(EditableSecurityStore securityStore) {
            this.securityStore = securityStore;
        }

        @Override
        public Collection<SecurityInfo> getAll() {
            return securityStore.getAll();
        }

        @Override
        public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
            return securityStore.add(info);
        }

        @Override
        public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
            return securityStore.remove(endpoint, infosAreCompromised);
        }

        @Override
        public void setListener(SecurityStoreListener listener) {
            securityStore.setListener(listener);
        }

        @Override
        public SecurityInfo getByEndpoint(String endpoint) {
            SecurityInfo securityInfo = securityStore.getByEndpoint(endpoint);
            if (securityInfo == null) {
                securityInfo = clientContext.addLwM2mClientToSession(endpoint).getSecurityInfo();
                try {
                    if (securityInfo != null) {
                        add(securityInfo);
                    }
                } catch (NonUniqueSecurityInfoException e) {
                    e.printStackTrace();
                }
            }
            return securityInfo;
        }

        @Override
        public SecurityInfo getByIdentity(String pskIdentity) {
            SecurityInfo securityInfo = securityStore.getByIdentity(pskIdentity);
            if (securityInfo == null) {
                securityInfo = clientContext.addLwM2mClientToSession(pskIdentity).getSecurityInfo();
                try {
                    if (securityInfo != null) {
                        add(securityInfo);
                    }
                } catch (NonUniqueSecurityInfoException e) {
                    e.printStackTrace();
                }
            }
            return securityInfo;
        }
    }
}
