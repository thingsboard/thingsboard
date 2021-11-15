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
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.BootstrapUtil;

import java.net.InetSocketAddress;
import java.util.*;

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
                        log.info(
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
                findServerInstanceId(readResponse);
                // create requests from config
                tasks.requestsToSend = this.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat(),
                        this.securityInstances.get(0));
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
        this.securityInstances = new HashMap<>();
        for (Link link : objectLinks) {
            if (link.getUrl().startsWith("/0/")) {
                try {
                    LwM2mPath path = new LwM2mPath(link.getUrl());
                    if (path.isObjectInstance()) {
                        if (link.getAttributes().containsKey("ssid")) {
                            int serverId = Integer.valueOf(link.getAttributes().get("ssid"));
                            if (!this.securityInstances.containsKey(serverId)) {
                                this.securityInstances.put(serverId, path.getObjectInstanceId());
                            } else {
                                log.error(String.format("Invalid lwm2mSecurityInstance by [{}]", path.getObjectInstanceId()));
                            }
                            this.securityInstances.put(Integer.valueOf(link.getAttributes().get("ssid")), path.getObjectInstanceId());
                        } else {
                            if (!this.securityInstances.containsKey(0)) {
                                this.securityInstances.put(0, path.getObjectInstanceId());
                            } else {
                                log.error(String.format("Invalid bootstrapSecurityInstance by [{}]", path.getObjectInstanceId()));
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
        serverInstances = new HashMap<>();
        ((LwM2mObject) readResponse.getContent()).getInstances().values().forEach(instance -> {
            serverInstances.put(((Long) instance.getResource(0).getValue()).intValue(), instance.getId());
        });
    }

    public BootstrapConfigStore getStore() {
        return this.store;
    }

    private void initAfterBootstrapDiscover(BootstrapDiscoverResponse response) {
        Link[] links = response.getObjectLinks();
        Arrays.stream(links).forEach(link -> {
            LwM2mPath path = new LwM2mPath(link.getUrl());
            if (path != null && !path.isRoot() && path.getObjectId() < 3) {
                if (path.isObject()) {
                    String ver = link.getAttributes().get("ver") != null ? link.getAttributes().get("ver") : "1.0";
                    this.supportedObjects.put(path.getObjectId(), ver);
                }
            }
        });
    }


    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> toRequests(BootstrapConfig bootstrapConfig,
                                                                                     ContentFormat contentFormat, int bootstrapSecurityInstanceId) {
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = new ArrayList<>();
        boolean isBsServer = false;
        boolean isLwServer = false;
        /** Map<serverId, InstanceId> */
        Map<Integer, Integer> instances = new HashMap<>();
        // handle security
        int id = 0;
        for (BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfig.security).values()) {
            if (security.bootstrapServer) {
                requests.add(toWriteRequest(bootstrapSecurityInstanceId, security, contentFormat));
                isBsServer = true;
                instances.put(security.serverId, bootstrapSecurityInstanceId);
            } else {
                if (id == bootstrapSecurityInstanceId) {
                    id++;
                }
                requests.add(toWriteRequest(id, security, contentFormat));
                instances.put(security.serverId, id);
                id++;
            }
        }
        // handle server
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> server : bootstrapConfig.servers.entrySet()) {
            int securityInstanceId = instances.get(server.getValue().shortId);
            requests.add(toWriteRequest(securityInstanceId, server.getValue(), contentFormat));
            if (!isBsServer && this.serverInstances.containsKey(server.getValue().shortId) && securityInstanceId != this.serverInstances.get(server.getValue().shortId)){
                requests.add(new BootstrapDeleteRequest("/0/" + securityInstanceId));
                requests.add(new BootstrapDeleteRequest("/1/" + this.serverInstances.get(server.getValue().shortId)));
            }
            isLwServer = true;
        }
        // handle acl
        for (Map.Entry<Integer, BootstrapConfig.ACLConfig> acl : bootstrapConfig.acls.entrySet()) {
            requests.add(toWriteRequest(acl.getKey(), acl.getValue(), contentFormat));
        }
        // handle delete
        if (isBsServer & isLwServer) {
            requests.add(new BootstrapDeleteRequest("/0"));
            requests.add(new BootstrapDeleteRequest("/1"));
        }

        return (requests);
    }

    private InetSocketAddress getSocketAddress(String uri) {
//        String uri1 = "coap://localhost:5687";
        if (uri.contains("coap://")) {
            uri = uri.replace("coap://", "");
        } else return null;
        String[] uris = uri.split(":");
        if (uris.length == 2) {
            try {
                return new InetSocketAddress(uris[0], Integer.parseInt(uris[1]));
            } catch (Exception e) {
                return null;
            }

        } else return null;
    }

    private void initSupportedObjectsDefault() {
        this.supportedObjects = new HashMap<>();
        this.supportedObjects.put(0, "1.1");
        this.supportedObjects.put(1, "1.1");
        this.supportedObjects.put(2, "1.0");
    }
}
