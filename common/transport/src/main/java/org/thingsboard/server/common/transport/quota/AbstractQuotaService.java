/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.quota;

import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryCleaner;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryLogger;
import org.thingsboard.server.common.transport.quota.inmemory.KeyBasedIntervalRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class AbstractQuotaService implements QuotaService {

    private final KeyBasedIntervalRegistry requestRegistry;
    private final RequestLimitPolicy requestsPolicy;
    private final IntervalRegistryCleaner registryCleaner;
    private final IntervalRegistryLogger registryLogger;
    private final boolean enabled;

    public AbstractQuotaService(KeyBasedIntervalRegistry requestRegistry, RequestLimitPolicy requestsPolicy,
                                    IntervalRegistryCleaner registryCleaner, IntervalRegistryLogger registryLogger,
                                    boolean enabled) {
        this.requestRegistry = requestRegistry;
        this.requestsPolicy = requestsPolicy;
        this.registryCleaner = registryCleaner;
        this.registryLogger = registryLogger;
        this.enabled = enabled;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            registryCleaner.schedule();
            registryLogger.schedule();
        }
    }

    @PreDestroy
    public void close() {
        if (enabled) {
            registryCleaner.stop();
            registryLogger.stop();
        }
    }

    @Override
    public boolean isQuotaExceeded(String key) {
        if (enabled) {
            long count = requestRegistry.tick(key);
            return !requestsPolicy.isValid(count);
        }
        return false;
    }
}
