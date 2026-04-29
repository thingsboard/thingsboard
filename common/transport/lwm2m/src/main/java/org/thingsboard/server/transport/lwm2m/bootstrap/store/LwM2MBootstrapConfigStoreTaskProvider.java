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
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapUtil;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.server.bootstrap.BootstrapUtil.toWriteRequest;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.Lwm2mServerIdentifier.LWM2M_SERVER_MAX;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.Lwm2mServerIdentifier.PRIMARY_LWM2M_SERVER;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.Lwm2mServerIdentifier.isLwm2mServer;

@Slf4j
public class LwM2MBootstrapConfigStoreTaskProvider implements LwM2MBootstrapTaskProvider {

    protected final ReadWriteLock readWriteLock;
    protected final Lock writeLock;

    private BootstrapConfigStore store;

    private Map<Integer, String> supportedObjects;

    /**
     * Map<sEndpoint, LwM2MBootstrapClientInstanceIds: securityInstances, serverInstances>
     */
    protected Map<String, LwM2MBootstrapClientInstanceIds> lwM2MBootstrapSessionClients;

    public LwM2MBootstrapConfigStoreTaskProvider(BootstrapConfigStore store) {
        this.store = store;
        this.lwM2MBootstrapSessionClients = new ConcurrentHashMap<>();
        readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
    }

    @Override
    public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponse) {
//        BootstrapConfig config = store.get(session.getEndpoint(), session.getClientTransportData().getIdentity(), session);
        BootstrapConfig configNew = store.get(session);
        if (configNew == null) {
            return null;
        }
        if (previousResponse == null && shouldStartWithDiscover(configNew)) {
            Tasks tasks = new Tasks();
            tasks.requestsToSend = new ArrayList<>(1);
            tasks.requestsToSend.add(new BootstrapDiscoverRequest());
            tasks.last = false;
            return tasks;
        } else {
            Tasks tasks = new Tasks();
            if (this.supportedObjects == null) {
                initSupportedObjectsDefault();
            }
            // add supportedObjects
            tasks.supportedObjects = this.supportedObjects;
            // handle bootstrap discover response
            if (previousResponse != null) {
                if (previousResponse.get(0) instanceof BootstrapDiscoverResponse discoverResponse) {
                    if (discoverResponse.isSuccess()) {
                         this.initAfterBootstrapDiscover(discoverResponse);
                        /// Short Server Ids - in old config
                        findInstancesIdOldByServerId(discoverResponse, session.getEndpoint());
                        log.warn(
                                "Bootstrap server instance successfully found in Security Object (0) in response {}. Continuing bootstrap session. Session: {}",
                                discoverResponse, session);
                    } else {
                        log.warn(
                                "Unable to find bootstrap server instance in Security Object (0) in response {}. Continuing bootstrap session with autoIdForSecurityObject mode, ignoring information from discoverResponse. Session: {}",
                                discoverResponse, session);
                    }
                }
                // create requests from config
                tasks.requestsToSend = this.toRequests(configNew,
                        configNew.contentFormat != null ? configNew.contentFormat : session.getContentFormat(), session.getEndpoint());
            } else {
                // create requests from config
                tasks.requestsToSend = BootstrapUtil.toRequests(configNew,
                        configNew.contentFormat != null ? configNew.contentFormat : session.getContentFormat());
            }
            return tasks;
        }
    }

    protected boolean shouldStartWithDiscover(BootstrapConfig config) {
        return config.autoIdForSecurityObject;
    }

    /**
     * "Short Server ID": This Resource MUST be set when the Bootstrap-Server Resource has a value of 'false'.
     * "Short Lwm2m Server ID":
     * - Link Instance (lwm2m Server) hase linkParams with key = "ssid" value = "shortId" (ver lvm2m = 1.1).
     * The values ID:0 values MUST NOT be used for identifying the LwM2M Server only BS.
     */
    protected void findInstancesIdOldByServerId(BootstrapDiscoverResponse discoverResponses, String endpoint) {
        log.info("Object after discover: [{}]", Arrays.toString(discoverResponses.getObjectLinks()));
        for (Link link : discoverResponses.getObjectLinks()) {
            LwM2mPath path = new LwM2mPath(link.getUriReference());
            if (path.isObjectInstance()) {
                int lwm2mShortServerId = 0;
                if (path.getObjectId() == 0) {
                    if (link.getAttributes().get("ssid") != null) {
                        lwm2mShortServerId = Integer.parseInt(link.getAttributes().get("ssid").getCoreLinkValue());
                        if (validateLwm2mShortServerId(lwm2mShortServerId)) {
                            this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().putIfAbsent(lwm2mShortServerId, path.getObjectInstanceId());
                        } else {
                            log.error("Invalid lwm2mSecurityInstance [{}] by short server id [{}]", path.getObjectInstanceId(), lwm2mShortServerId);
                        }
                    } else {
                        this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().putIfAbsent(null, path.getObjectInstanceId());
                    }
                } else if (path.getObjectId() == 1) {
                    if (link.getAttributes().get("ssid") != null) {
                        lwm2mShortServerId = Integer.parseInt(link.getAttributes().get("ssid").getCoreLinkValue());
                        if (validateLwm2mShortServerId(lwm2mShortServerId)) {
                            this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().putIfAbsent(lwm2mShortServerId, path.getObjectInstanceId());
                        } else {
                            log.error("Invalid lwm2mServerInstance [{}] by short server id [{}]", path.getObjectInstanceId(), lwm2mShortServerId);
                        }
                    }
                }
            }
        }
    }

    public BootstrapConfigStore getStore() {
        return this.store;
    }

    private void initAfterBootstrapDiscover(BootstrapDiscoverResponse response) {
        Link[] links = response.getObjectLinks();
        AtomicReference<String> verDefault = new AtomicReference<>("1.0");
        Arrays.stream(links).forEach(link -> {
            LwM2mPath path = new LwM2mPath(link.getUriReference());
            if (path.isRoot()) {
                if (link.hasAttribute() && link.getAttributes().get("lwm2m") != null) {
                    verDefault.set(link.getAttributes().get("lwm2m").getValue().toString());
                }
            } else if (path.getObjectId() <= ACCESS_CONTROL) {
                if (path.isObject()) {
                    String ver = (link.hasAttribute() && link.getAttributes().get("ver") != null) ? link.getAttributes().get("ver").getCoreLinkValue() : verDefault.get();
                    this.supportedObjects.put(path.getObjectId(), ver);
                }
            }
        });
    }


    /** Map<serverId ("Short Server ID"), InstanceId> => LwM2MBootstrapClientInstanceIds
     * 1) Both
     * - (Short) Server ID == null bs)
     *  SECURITY = 0; InstanceId = 0
     * - Short Server ID == 1 - 65534 lwm2m)
     *  SECURITY = 0; InstanceId = 1
     *  SERVER = 1; InstanceId = 0
     * 2) Only BS Server
     * - Short Server ID == null bs)
     *  SECURITY = 0; InstanceId = 0
     * 3) Only Lwm2m Server
     * - Short Server ID == 1 - 65534 lwm2m)
     *  SECURITY = 0; InstanceId = 0
     *  SERVER = 1; InstanceId = 0
     * */
    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> toRequests(BootstrapConfig bootstrapConfigNew,
                                                                              ContentFormat contentFormat,
                                                                              String endpoint) {
        Integer bootstrapSecurityInstanceId = this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(null) == null ?
                -2 : this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(null);
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = new ArrayList<>();
        Set<String> pathsDelete = new HashSet<>();
        ConcurrentHashMap<String, BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsWrite = new ConcurrentHashMap<>();

        /// handle security & handle
        // bootstrap  Security new - There can only be one instance of bootstrap  at a time.
        /// bs: handle security only
        for (BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfigNew.security).values()) {
            if (security.bootstrapServer && bootstrapSecurityInstanceId > -1) {
                // delete old bootstrap Security
                String path = "/" + SECURITY + "/" + bootstrapSecurityInstanceId;
                pathsDelete.add(path);
                security.serverId = null;
                requestsWrite.put(path, toWriteRequest(bootstrapSecurityInstanceId, security, contentFormat));
            }
        }

        /** lwm2m servers: Multiple instances of lwm2m servers can run simultaneously by SHORT_ID
        if update -> delete and write by InstanceId
        if new -> only write with InstanceIdMax++
         */

        /// lwm2m server: handle security & server
        //max Lwm2m Security instance old id if new
        int lwm2mSecurityInstanceIdMax = -1;
        for (Integer shortId : this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().keySet()) {
            if (isLwm2mServer(shortId)) {
                lwm2mSecurityInstanceIdMax = Math.max(
                        this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(shortId),
                        lwm2mSecurityInstanceIdMax);
            }
        }
        //max Lwm2m Server instance old id if new
        int lwm2mServerInstanceIdMax = -1;
        for (Integer shortId : this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().keySet()) {
            if (isLwm2mServer(shortId)) {
                lwm2mServerInstanceIdMax = Math.max(
                        this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(shortId),
                        lwm2mServerInstanceIdMax);
            }
        }
        // Lwm2m update or new
        for (BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfigNew.security).values()) {
            if (!security.bootstrapServer) {
                // Security
                Integer secureInstanceId = this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId);
                if (secureInstanceId != null) {
                    pathsDelete.add("/" + SECURITY + "/" + secureInstanceId);
                    requestsWrite.put("/" + SECURITY + "/" + secureInstanceId, toWriteRequest(secureInstanceId, security, contentFormat));
                } else {
                    secureInstanceId = ++lwm2mSecurityInstanceIdMax;
                    if (bootstrapSecurityInstanceId.equals(secureInstanceId)) {
                        secureInstanceId = ++lwm2mSecurityInstanceIdMax;
                    }
                    requestsWrite.put("/" + SECURITY + "/" + secureInstanceId, toWriteRequest(secureInstanceId, security, contentFormat));
                }
                Integer serverInstanceId = this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(security.serverId);
                if (serverInstanceId != null) {
                    pathsDelete.add("/" + SERVER + "/" + serverInstanceId);
                } else {
                    serverInstanceId = ++lwm2mServerInstanceIdMax;
                }
                Integer finalServerInstanceId = serverInstanceId;
                new TreeMap<>(bootstrapConfigNew.servers).values().stream()
                        .filter(server -> server.shortId == security.serverId)
                        .findFirst()
                        .ifPresent(server ->
                                requestsWrite.put(
                                        "/" + SERVER + "/" + finalServerInstanceId,
                                        toWriteRequest(finalServerInstanceId, server, contentFormat)
                                )
                        );
            }
        }

        /// handle acl
        for (Map.Entry<Integer, BootstrapConfig.ACLConfig> acl : bootstrapConfigNew.acls.entrySet()) {
            requestsWrite.put("/" + ACCESS_CONTROL + "/" + acl.getKey(), toWriteRequest(acl.getKey(), acl.getValue(), contentFormat));
        }
        /// handle delete
        pathsDelete.forEach(pathDelete -> requests.add(new BootstrapDeleteRequest(pathDelete)));

        /// handle write
        if (!requestsWrite.isEmpty()) {
            requests.addAll(requestsWrite.values());
        }
        return (requests);
    }

    private void initSupportedObjectsDefault() {
        this.supportedObjects = new HashMap<>();
        this.supportedObjects.put(SECURITY, "1.1");
        this.supportedObjects.put(SERVER, "1.1");
        this.supportedObjects.put(ACCESS_CONTROL, "1.0");
    }

    private boolean validateLwm2mShortServerId(int id){
        return  id >= PRIMARY_LWM2M_SERVER.getId() && id <= LWM2M_SERVER_MAX.getId();
    }

    @Override
    public void remove(String endpoint) {
        writeLock.lock();
        try {
            this.lwM2MBootstrapSessionClients.remove(endpoint);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void put(String endpoint) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            this.lwM2MBootstrapSessionClients.put(endpoint, new LwM2MBootstrapClientInstanceIds());
        } finally {
            writeLock.unlock();
        }
    }
}
