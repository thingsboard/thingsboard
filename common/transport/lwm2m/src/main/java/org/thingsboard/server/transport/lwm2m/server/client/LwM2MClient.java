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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportServiceImpl;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class LwM2MClient implements Cloneable {
    private String deviceName;
    private String deviceProfileName;
    private String endPoint;
    private String identity;
    private SecurityInfo securityInfo;
    private UUID deviceUuid;
    private UUID sessionUuid;
    private UUID profileUuid;
    private LeshanServer lwServer;
    private LwM2MTransportServiceImpl lwM2MTransportServiceImpl;
    private Registration registration;
    private ValidateDeviceCredentialsResponseMsg credentialsResponse;
    private Map<String, String> attributes;
    private Map<String, ResourceValue> resources;
    private Map<String, TransportProtos.TsKvProto> delayedRequests;
    private ArrayList<String> pendingRequests;
    private boolean init;
    private final LwM2mValueConverterImpl converter;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public LwM2MClient(String endPoint, String identity, SecurityInfo securityInfo, ValidateDeviceCredentialsResponseMsg credentialsResponse, UUID profileUuid, UUID sessionUuid) {
        this.endPoint = endPoint;
        this.identity = identity;
        this.securityInfo = securityInfo;
        this.credentialsResponse = credentialsResponse;
        this.attributes = new ConcurrentHashMap<>();
        this.delayedRequests = new ConcurrentHashMap<>();
        this.pendingRequests = new ArrayList<>();
        this.resources = new ConcurrentHashMap<>();
        this.profileUuid = profileUuid;
        this.sessionUuid = sessionUuid;
        this.converter = LwM2mValueConverterImpl.getInstance();
        this.init = false;
    }

    public void updateResourceValue(String pathRez, LwM2mResource rez) {
        if (rez instanceof LwM2mMultipleResource) {
            this.resources.put(pathRez, new ResourceValue(rez.getValues(), null, true));
        } else if (rez instanceof LwM2mSingleResource) {
            this.resources.put(pathRez, new ResourceValue(null, rez.getValue(), false));
        }
    }

    public void initValue(LwM2MTransportServiceImpl lwM2MTransportService, String path) {
        if (path != null) {
            this.pendingRequests.remove(path);
        }
        if (this.pendingRequests.size() == 0) {
            this.init = true;
            lwM2MTransportService.putDelayedUpdateResourcesThingsboard(this);
        }
    }

    public LwM2MClient copy() {
        return new LwM2MClient(this.endPoint, this.identity, this.securityInfo, this.credentialsResponse, this.profileUuid, this.sessionUuid);
    }
}

