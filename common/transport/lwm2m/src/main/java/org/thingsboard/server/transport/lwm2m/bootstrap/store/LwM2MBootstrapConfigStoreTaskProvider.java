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
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapUtil;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.server.bootstrap.BootstrapUtil.toWriteRequest;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.BOOTSTRAP_DEFAULT_SHORT_ID_0;

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
        BootstrapConfig config = store.get(session);
        if (config == null) {
            return null;
        }
        if (previousResponse == null && shouldStartWithDiscover(config)) {
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
                if (previousResponse.get(0) instanceof BootstrapDiscoverResponse) {
                    BootstrapDiscoverResponse discoverResponse = (BootstrapDiscoverResponse) previousResponse.get(0);
                    if (discoverResponse.isSuccess()) {
                        this.initAfterBootstrapDiscover(discoverResponse);
                        findSecurityInstanceId(discoverResponse.getObjectLinks(), session.getEndpoint());
                    } else {
                        log.warn(
                                "Bootstrap Discover return error {} : to continue bootstrap session without autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                    }
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID_0) == null) {
                        log.error(
                                "Unable to find bootstrap server instance in Security Object (0) in response {}: unable to continue bootstrap session with autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                        return null;
                    }
                    tasks.requestsToSend = new ArrayList<>(1);
                    tasks.requestsToSend.add(new BootstrapReadRequest("/1"));
                    tasks.last = false;
                    return tasks;
                }
                BootstrapReadResponse readResponse = (BootstrapReadResponse) previousResponse.get(0);
                Integer bootstrapServerIdOld = null;
                if (readResponse.isSuccess()) {
                    findServerInstanceId(readResponse, session.getEndpoint());
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().size() > 0 && this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getServerInstances().size() > 0) {
                        bootstrapServerIdOld = this.findBootstrapServerId(session.getEndpoint());
                    }
                } else {
                    log.warn(
                            "Bootstrap ReadResponse return error {} : to continue bootstrap session without find Server Instance Id. {}",
                            readResponse, session);
                }
                // create requests from config
                tasks.requestsToSend = this.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat(),
                        bootstrapServerIdOld, session.getEndpoint());
            } else {
                // create requests from config
                tasks.requestsToSend = BootstrapUtil.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat());
            }
            return tasks;
        }
    }

    protected boolean shouldStartWithDiscover(BootstrapConfig config) {
        return config.autoIdForSecurityObject;
    }

    /**
     * "Short Server ID": This Resource MUST be set when the Bootstrap-Server Resource has a value of 'false'.
     * The values ID:0 and ID:65535 values MUST NOT be used for identifying the LwM2M Server.
     * "Short Server ID":
     * - Link Instance (lwm2m Server) hase linkParams with key = "ssid" value = "shortId" (ver lvm2m = 1.1).
     * - Link Instance (bootstrap Server) hase not linkParams with key = "ssid" (ver lvm2m = 1.0).
     */
    protected void findSecurityInstanceId(Link[] objectLinks, String endpoint) {
        log.info("Object after discover: [{}]", objectLinks);
        for (Link link : objectLinks) {
            if (link.getUriReference().startsWith("/0/")) {
                try {
                    LwM2mPath path = new LwM2mPath(link.getUriReference());
                    if (path.isObjectInstance()) {
                        if (link.getAttributes().get("ssid") != null) {
                            int serverId = Integer.parseInt(link.getAttributes().get("ssid").getCoreLinkValue());
                            if (!lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(serverId)) {
                                lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(serverId, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid lwm2mSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                            lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(serverId, path.getObjectInstanceId());
                        } else {
                            if (!this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(0)) {
                                this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(BOOTSTRAP_DEFAULT_SHORT_ID_0, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid bootstrapSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore if this is not a LWM2M path
                    log.error("Invalid LwM2MPath starting by \"/0/\"");
                }
            }
        }
    }

    protected void findServerInstanceId(BootstrapReadResponse readResponse, String endpoint) {
        try {
            ((LwM2mObject) readResponse.getContent()).getInstances().values().forEach(instance -> {
                var shId = OPAQUE.equals(instance.getResource(0).getType()) ? new BigInteger((byte[]) instance.getResource(0).getValue()).intValue() : instance.getResource(0).getValue();
                int shortId;
                if (shId instanceof Long) {
                    shortId = ((Long) shId).intValue();
                } else {
                    shortId = (int) shId;
                }
                this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().put(shortId, instance.getId());
            });
        } catch (Exception e) {
            log.error("Failed find Server Instance Id. ", e);
        }
    }

    protected Integer findBootstrapServerId(String endpoint) {
        Integer bootstrapServerIdOld = null;
        Map<Integer, Integer> filteredMap = this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().entrySet()
                .stream().filter(x -> !this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(x.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (filteredMap.size() > 0) {
            bootstrapServerIdOld = filteredMap.keySet().stream().findFirst().get();
        }
        return bootstrapServerIdOld;
    }

    public BootstrapConfigStore getStore() {
        return this.store;
    }

    private void initAfterBootstrapDiscover(BootstrapDiscoverResponse response) {
        Link[] links = response.getObjectLinks();
        Arrays.stream(links).forEach(link -> {
            LwM2mPath path = new LwM2mPath(link.getUriReference());
            if (!path.isRoot() && path.getObjectId() < 3) {
                if (path.isObject()) {
                    String ver = link.getAttributes().get("ver") != null ? link.getAttributes().get("ver").getCoreLinkValue() : "1.0";
                    this.supportedObjects.put(path.getObjectId(), ver);
                }
            }
        });
    }


    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> toRequests(BootstrapConfig bootstrapConfig,
                                                                              ContentFormat contentFormat,
                                                                              Integer bootstrapServerIdOld,
                                                                              String endpoint) {
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = new ArrayList<>();
        Set<String> pathsDelete = new HashSet<>();
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsWrite = new ArrayList<>();
        boolean isBsServer = false;
        boolean isLwServer = false;
        /** Map<serverId ("Short Server ID"), InstanceId> */
        Map<Integer, Integer> instances = new HashMap<>();
        Integer bootstrapServerIdNew = null;
        // handle security
        int lwm2mSecurityInstanceId = 0;
        int bootstrapSecurityInstanceId = this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID_0);
        for (BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfig.security).values()) {
            if (security.bootstrapServer) {
                requestsWrite.add(toWriteRequest(bootstrapSecurityInstanceId, security, contentFormat));
                isBsServer = true;
                bootstrapServerIdNew = security.serverId;
                instances.put(security.serverId, bootstrapSecurityInstanceId);
            } else {
                if (lwm2mSecurityInstanceId == bootstrapSecurityInstanceId) {
                    lwm2mSecurityInstanceId++;
                }
                requestsWrite.add(toWriteRequest(lwm2mSecurityInstanceId, security, contentFormat));
                instances.put(security.serverId, lwm2mSecurityInstanceId);
                isLwServer = true;
                if (!isBsServer && this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(security.serverId) &&
                        lwm2mSecurityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId)) {
                    pathsDelete.add("/0/" + this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId));
                }
                /**
                 * If there is an instance in the serverInstances with serverId which we replace in the securityInstances
                 */
                // find serverId in securityInstances by id (instance)
                Integer serverIdOld = null;
                for (Map.Entry<Integer, Integer> entry : this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().entrySet()) {
                    if (entry.getValue().equals(lwm2mSecurityInstanceId)) {
                        serverIdOld = entry.getKey();
                    }
                }
                if (!isBsServer && serverIdOld != null && this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(serverIdOld)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(serverIdOld));
                }
                lwm2mSecurityInstanceId++;
            }
        }
        // handle server
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> server : bootstrapConfig.servers.entrySet()) {
            int securityInstanceId = instances.get(server.getValue().shortId);
            requestsWrite.add(toWriteRequest(securityInstanceId, server.getValue(), contentFormat));
            if (!isBsServer) {
                /** Delete instance if bootstrapServerIdNew not equals bootstrapServerIdOld or securityInstanceBsIdNew not equals serverInstanceBsIdOld */
                if (bootstrapServerIdNew != null && server.getValue().shortId == bootstrapServerIdNew &&
                        (bootstrapServerIdNew != bootstrapServerIdOld || securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld))) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld));
                    /** Delete instance if serverIdNew is present in serverInstances and  securityInstanceIdOld by serverIdNew not equals serverInstanceIdOld */
                } else if (this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(server.getValue().shortId) &&
                        securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId));
                }
            }
        }
        // handle acl
        for (Map.Entry<Integer, BootstrapConfig.ACLConfig> acl : bootstrapConfig.acls.entrySet()) {
            requestsWrite.add(toWriteRequest(acl.getKey(), acl.getValue(), contentFormat));
        }
        // handle delete
        if (isBsServer && isLwServer) {
            requests.add(new BootstrapDeleteRequest("/0"));
            requests.add(new BootstrapDeleteRequest("/1"));
        } else {
            pathsDelete.forEach(pathDelete -> requests.add(new BootstrapDeleteRequest(pathDelete)));
        }
        // handle write
        if (requestsWrite.size() > 0) {
            requests.addAll(requestsWrite);
        }
        return (requests);
    }


    private void initSupportedObjectsDefault() {
        this.supportedObjects = new HashMap<>();
        this.supportedObjects.put(0, "1.1");
        this.supportedObjects.put(1, "1.1");
        this.supportedObjects.put(2, "1.0");
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
