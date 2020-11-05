/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.secure;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MGetSecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.ReadResultSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportService;
import org.thingsboard.server.transport.lwm2m.server.client.ModelClient;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;

@Slf4j
@Component("LwM2mInMemorySecurityStore")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' )|| ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2mInMemorySecurityStore extends InMemorySecurityStore {
    // lock for the two maps
    protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();
    private final boolean infosAreCompromised = false;

    protected Map<String /** registrationId */, ModelClient> sessions = new ConcurrentHashMap<>();
    protected Set<String> removeSessions = ConcurrentHashMap.newKeySet();
    private SecurityStoreListener listener;

    @Autowired
    LwM2MGetSecurityInfo lwM2MGetSecurityInfo;

    @Override
    public SecurityInfo getByEndpoint(String endPoint) {
        readLock.lock();
        try {
            String integrationId = this.getByRegistrationId(endPoint, null);
            return (integrationId != null) ? sessions.get(integrationId).getInfo() : add(endPoint);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        readLock.lock();
        try {
            String integrationId = this.getByRegistrationId(null, identity);
            return (integrationId != null) ? sessions.get(integrationId).getInfo() : add(identity);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        readLock.lock();
        try {
            return Collections.unmodifiableCollection(this.sessions.entrySet().stream().map(model -> model.getValue().getInfo()).collect(Collectors.toList()));
        } finally {
            readLock.unlock();
        }
    }

    public void setRemoveSessions(String registrationId) {
        removeSessions.add(registrationId);
    }

    public void remove() {
        try {
            removeSessions.stream().forEach(regId -> {
                removeOne(regId);
                removeSessions.remove(regId);
            });
        } finally {
        }
    }

    private SecurityInfo removeOne(String registrationId) {
        writeLock.lock();
        try {
            ModelClient modelClient = (sessions.get(registrationId) != null) ? sessions.get(registrationId) : null;
            SecurityInfo info = null;
            if (modelClient != null) {
                info = modelClient.getInfo();
                if (listener != null) {
                    listener.securityInfoRemoved(infosAreCompromised, info);

                }
                sessions.remove(registrationId);
            }
            return info;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        this.listener = listener;
    }

    public ModelClient getByModelClient(String endPoint, String identity) {
        Map.Entry<String, ModelClient> modelClients = (endPoint != null) ?
                this.sessions.entrySet().stream().filter(model -> endPoint.equals(model.getValue().getEndPoint())).findAny().orElse(null) :
                this.sessions.entrySet().stream().filter(model -> identity.equals(model.getValue().getIdentity())).findAny().orElse(null);
        return (modelClients != null) ? modelClients.getValue() : null;
    }

    public ModelClient getByRegistrationIdModelClient(String registrationId) {
        return this.sessions.get(registrationId);
    }

    private String getByRegistrationId(String endPoint, String identity) {
        List<String> registrationIds = (endPoint != null) ?
                this.sessions.entrySet().stream().filter(model -> endPoint.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()) :
                this.sessions.entrySet().stream().filter(model -> identity.equals(model.getValue().getIdentity())).map(model -> model.getKey()).collect(Collectors.toList());
        return (registrationIds != null && registrationIds.size() > 0) ? registrationIds.get(0) : null;
    }

    public String getByRegistrationId(String credentialsId) {
        List<String> registrationIds = (this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()).size()>0) ?
                this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getEndPoint())).map(model -> model.getKey()).collect(Collectors.toList()) :
                this.sessions.entrySet().stream().filter(model -> credentialsId.equals(model.getValue().getIdentity())).map(model -> model.getKey()).collect(Collectors.toList());
        return (registrationIds != null && registrationIds.size() > 0) ? registrationIds.get(0) : null;
    }

    public Registration getByRegistration (String registrationId) {
        return this.sessions.get(registrationId).getRegistration();
    }

    private SecurityInfo add(String identity) {
        ReadResultSecurityStore store = lwM2MGetSecurityInfo.getSecurityInfo(identity, TypeServer.CLIENT);
        if (store.getSecurityInfo() != null) {
            if (store.getSecurityMode() < DEFAULT_MODE.code) {
                String endpoint = store.getSecurityInfo().getEndpoint();
                sessions.put(endpoint, new ModelClient(endpoint, store.getSecurityInfo().getIdentity(), store.getSecurityInfo(), store.getMsg(), null, null));
            }
        } else {
            if (store.getSecurityMode() == NO_SEC.code)
                sessions.put(identity, new ModelClient(identity, null, null, store.getMsg(), null, null));
            else log.error("Registration failed: FORBIDDEN, endpointId: [{}]", identity);
        }
        return store.getSecurityInfo();
    }

    @SneakyThrows
    public ModelClient replaceNewRegistration(LeshanServer lwServer, Registration registration, LwM2MTransportService transportService) {
        writeLock.lock();
        try {
            ModelClient modelClient = null;
            if (this.sessions.get(registration.getEndpoint()) != null &&
                    this.sessions.get(registration.getEndpoint()).clone() != null) {
                modelClient = (ModelClient) this.sessions.get(registration.getEndpoint()).clone();
                modelClient.setRegistrationParam(lwServer, registration);
                modelClient.setAttributes(registration.getAdditionalRegistrationAttributes());
                this.sessions.put(registration.getId(), modelClient);
                this.sessions.remove(registration.getEndpoint());
            }
            return modelClient;
        } finally {
            writeLock.unlock();
        }
    }

    public Map<String, ModelClient> getSessions () {
        return  this.sessions;
    }


}
