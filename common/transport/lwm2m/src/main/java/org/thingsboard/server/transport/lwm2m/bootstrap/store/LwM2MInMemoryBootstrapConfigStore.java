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
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.ConfigurationChecker;
import org.eclipse.leshan.server.bootstrap.InMemoryBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Component("LwM2MInMemoryBootstrapConfigStore")
@TbLwM2mBootstrapTransportComponent
public class LwM2MInMemoryBootstrapConfigStore extends InMemoryBootstrapConfigStore {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    protected final ConfigurationChecker configChecker = new LwM2MConfigurationChecker();


    public BootstrapConfig get(String endpoint) {
        return bootstrapByEndpoint.get(endpoint);
    }

    @Override
    public Map<String, BootstrapConfig> getAll() {
        readLock.lock();
        try {
            return super.getAll();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void add(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            addToStore(endpoint, config);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public BootstrapConfig remove(String endpoint) {
        writeLock.lock();
        try {
            return super.remove(endpoint);
        } finally {
            writeLock.unlock();
        }
    }

    public void addToStore(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        configChecker.verify(config);
        // Check PSK identity uniqueness for bootstrap server:
        PskByServer pskToAdd = getBootstrapPskIdentity(config);
        if (pskToAdd != null) {
            BootstrapConfig existingConfig = bootstrapByPskId.get(pskToAdd);
            if (existingConfig != null) {
                // check if this config will be replace by the new one.
                BootstrapConfig previousConfig = bootstrapByEndpoint.get(endpoint);
                if (previousConfig != existingConfig) {
                    throw new InvalidConfigurationException(
                            "Psk identity [%s] already used for this bootstrap server [%s]", pskToAdd.identity,
                            pskToAdd.serverUrl);
                }
            }
        }

        bootstrapByEndpoint.put(endpoint, config);
        if (pskToAdd != null) {
            bootstrapByPskId.put(pskToAdd, config);
        }
    }
}
