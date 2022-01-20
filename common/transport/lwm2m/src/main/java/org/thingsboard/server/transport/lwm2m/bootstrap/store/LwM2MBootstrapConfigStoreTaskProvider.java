/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.BootstrapUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.eclipse.leshan.server.bootstrap.BootstrapUtil.toWriteRequest;

@Slf4j
public class LwM2MBootstrapConfigStoreTaskProvider implements BootstrapTaskProvider {

    private BootstrapConfigStore store;

    private Map<Integer, String> supportedObjects;

    /**
     * Map<serverId, InstanceId>
     */
    protected Map<Integer, Integer> securityInstances;
    protected Map<Integer, Integer> serverInstances;
    protected Integer bootstrapServerIdOld;
    protected Integer bootstrapServerIdNew;

    public LwM2MBootstrapConfigStoreTaskProvider(BootstrapConfigStore store) {
        this.store = store;
    }

    @Override
    public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponse) {
        BootstrapConfig config = store.get(session.getEndpoint(), session.getIdentity(), session);
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
                        findSecurityInstanceId(discoverResponse.getObjectLinks());
                    } else {
                        log.warn(
                                "Bootstrap Discover return error {} : to continue bootstrap session without autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                    }
                    if (this.securityInstances.get(0) == null) {
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
                if (readResponse.isSuccess()) {
                    findServerInstanceId(readResponse);
                } else {
                    log.warn(
                            "Bootstrap ReadResponse return error {} : to continue bootstrap session without find Server Instance Id. {}",
                            readResponse, session);
                }
                // create requests from config
                tasks.requestsToSend = this.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat());
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

    protected void findSecurityInstanceId(Link[] objectLinks) {
        log.info("Object after discover: [{}]", objectLinks);
        this.securityInstances = new HashMap<>();
        for (Link link : objectLinks) {
            if (link.getUriReference().startsWith("/0/")) {
                try {
                    LwM2mPath path = new LwM2mPath(link.getUriReference());
                    if (path.isObjectInstance()) {
                        if (link.getLinkParams().containsKey("ssid")) {
                            int serverId = Integer.parseInt(link.getLinkParams().get("ssid").getUnquoted());
                            if (!this.securityInstances.containsKey(serverId)) {
                                this.securityInstances.put(serverId, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid lwm2mSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                            this.securityInstances.put(Integer.valueOf(link.getLinkParams().get("ssid").getUnquoted()), path.getObjectInstanceId());
                        } else {
                            if (!this.securityInstances.containsKey(0)) {
                                this.securityInstances.put(0, path.getObjectInstanceId());
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

    protected void findServerInstanceId(BootstrapReadResponse readResponse) {
        this.serverInstances = new HashMap<>();
        ((LwM2mObject) readResponse.getContent()).getInstances().values().forEach(instance -> {
            serverInstances.put(((Long) instance.getResource(0).getValue()).intValue(), instance.getId());
        });
        if (this.securityInstances != null && this.securityInstances.size() > 0 && this.serverInstances != null && this.serverInstances.size() > 0) {
            this.findBootstrapServerId();
        }
    }

    protected void findBootstrapServerId() {
        Map<Integer, Integer> filteredMap = this.serverInstances.entrySet()
                .stream().filter(x -> !this.securityInstances.containsKey(x.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (filteredMap.size() > 0) {
            this.bootstrapServerIdOld = filteredMap.keySet().stream().findFirst().get();
        }
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
                    String ver = link.getLinkParams().get("ver") != null ? link.getLinkParams().get("ver").getUnquoted() : "1.0";
                    this.supportedObjects.put(path.getObjectId(), ver);
                }
            }
        });
    }


    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> toRequests(BootstrapConfig bootstrapConfig,
                                                                              ContentFormat contentFormat) {
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = new ArrayList<>();
        Set<String> pathsDelete = new HashSet<>();
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsWrite = new ArrayList<>();
        boolean isBsServer = false;
        boolean isLwServer = false;
        /** Map<serverId, InstanceId> */
        Map<Integer, Integer> instances = new HashMap<>();
        // handle security
        int id = 0;
        for (BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfig.security).values()) {
            if (security.bootstrapServer) {
                requestsWrite.add(toWriteRequest(this.securityInstances.get(0), security, contentFormat));
                isBsServer = true;
                this.bootstrapServerIdNew = security.serverId;
                instances.put(security.serverId, this.securityInstances.get(0));
            } else {
                if (id == this.securityInstances.get(0)) {
                    id++;
                }
                requestsWrite.add(toWriteRequest(id, security, contentFormat));
                instances.put(security.serverId, id);
                isLwServer = true;
                if (!isBsServer && this.securityInstances.containsKey(security.serverId) && id != this.securityInstances.get(security.serverId)) {
                    pathsDelete.add("/0/" + this.securityInstances.get(security.serverId));
                }
                /**
                 * If there is an instance in the serverInstances with serverId which we replace in the securityInstances
                 */
                // find serverId in securityInstances by id (instance)
                Integer serverIdOld = null;
                for (Map.Entry<Integer, Integer> entry : this.securityInstances.entrySet()) {
                    if (entry.getValue().equals(id)) {
                        serverIdOld = entry.getKey();
                    }
                }
                if (!isBsServer && serverIdOld != null && this.serverInstances.containsKey(serverIdOld)) {
                    pathsDelete.add("/1/" + this.serverInstances.get(serverIdOld));
                }
                id++;
            }
        }
        // handle server
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> server : bootstrapConfig.servers.entrySet()) {
            int securityInstanceId = instances.get(server.getValue().shortId);
            requestsWrite.add(toWriteRequest(securityInstanceId, server.getValue(), contentFormat));
            if (!isBsServer) {
                /** Delete instance if bootstrapServerIdNew not equals bootstrapServerIdOld or securityInstanceBsIdNew not equals serverInstanceBsIdOld */
                if (this.bootstrapServerIdNew != null && server.getValue().shortId == this.bootstrapServerIdNew &&
                        (this.bootstrapServerIdNew != this.bootstrapServerIdOld || securityInstanceId != this.serverInstances.get(this.bootstrapServerIdOld))) {
                    pathsDelete.add("/1/" + this.serverInstances.get(this.bootstrapServerIdOld));
                /** Delete instance if serverIdNew is present in serverInstances and  securityInstanceIdOld by serverIdNew not equals serverInstanceIdOld */
                } else if (this.serverInstances.containsKey(server.getValue().shortId) && securityInstanceId != this.serverInstances.get(server.getValue().shortId)) {
                    pathsDelete.add("/1/" + this.serverInstances.get(server.getValue().shortId));
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
}
