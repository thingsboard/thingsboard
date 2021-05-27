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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.Collection;

@Slf4j
@Component
@TbLwM2mTransportComponent
public class TbLwM2mSecurityStore implements EditableSecurityStore {

    private final LwM2mClientContext clientContext;
    private final EditableSecurityStore securityStore;

    public TbLwM2mSecurityStore(LwM2mClientContext clientContext, EditableSecurityStore securityStore) {
        this.clientContext = clientContext;
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
            LwM2mClient lwM2mClient = clientContext.getClientByEndpoint(endpoint);
            if (lwM2mClient != null && lwM2mClient.getRegistration() != null && !lwM2mClient.getRegistration().getIdentity().isSecure()) {
                return null;
            }
            securityInfo = clientContext.fetchClientByEndpoint(endpoint).getSecurityInfo();
            try {
                if (securityInfo != null) {
                    add(securityInfo);
                }
            } catch (NonUniqueSecurityInfoException e) {
                log.trace("Failed to add security info: {}", securityInfo, e);
            }
        }
        return securityInfo;
    }

    @Override
    public SecurityInfo getByIdentity(String pskIdentity) {
        SecurityInfo securityInfo = securityStore.getByIdentity(pskIdentity);
        if (securityInfo == null) {
            securityInfo = clientContext.fetchClientByEndpoint(pskIdentity).getSecurityInfo();
            try {
                if (securityInfo != null) {
                    add(securityInfo);
                }
            } catch (NonUniqueSecurityInfoException e) {
                log.trace("Failed to add security info: {}", securityInfo, e);
            }
        }
        return securityInfo;
    }
}
