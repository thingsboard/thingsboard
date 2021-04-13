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
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportHandler.convertToObjectIdFromIdVer;

@Slf4j
@Data
public class LwM2mClient implements Cloneable {
    private String deviceName;
    private String deviceProfileName;
    private String endpoint;
    private String identity;
    private SecurityInfo securityInfo;
    private UUID deviceId;
    private UUID sessionId;
    private UUID profileId;
    private Registration registration;
    private ValidateDeviceCredentialsResponseMsg credentialsResponse;
    private final Map<String, ResourceValue> resources;
    private final Map<String, TransportProtos.TsKvProto> delayedRequests;
    private final List<String> pendingRequests;
    private boolean init;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public LwM2mClient(String endpoint, String identity, SecurityInfo securityInfo, ValidateDeviceCredentialsResponseMsg credentialsResponse, UUID profileId, UUID sessionId) {
        this.endpoint = endpoint;
        this.identity = identity;
        this.securityInfo = securityInfo;
        this.credentialsResponse = credentialsResponse;
        this.delayedRequests = new ConcurrentHashMap<>();
        this.pendingRequests = new CopyOnWriteArrayList<>();
        this.resources = new ConcurrentHashMap<>();
        this.profileId = profileId;
        this.sessionId = sessionId;
        this.init = false;
    }

    public boolean saveResourceValue(String pathRez, LwM2mResource rez, LwM2mModelProvider modelProvider) {
        if (this.resources.get(pathRez) != null && this.resources.get(pathRez).getResourceModel() != null) {
            this.resources.get(pathRez).setLwM2mResource(rez);
            return true;
        } else {
            LwM2mPath pathIds = new LwM2mPath(convertToObjectIdFromIdVer(pathRez));
            ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
            if (resourceModel != null) {
                this.resources.put(pathRez, new ResourceValue(rez, resourceModel));
                return true;
            } else {
                return false;
            }
        }
    }

    public ResourceModel getResourceModel(String pathRez) {
        if (this.getResources().get(pathRez) != null) {
            return this.getResources().get(pathRez).getResourceModel();
        } else {
            return null;
        }
    }

    /**
     *
     * @param pathIdVer == "3_1.0"
     * @param modelProvider -
     */
    public void deleteResources(String pathIdVer, LwM2mModelProvider modelProvider) {
        Set<String> key = getKeysEqualsIdVer(pathIdVer);
        key.forEach(pathRez -> {
            LwM2mPath pathIds = new LwM2mPath(convertToObjectIdFromIdVer(pathRez));
            ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
            if (resourceModel != null) {
                this.resources.get(pathRez).setResourceModel(resourceModel);
            }
            else {
                this.resources.remove(pathRez);
            }
        });
    }

    /**
     *
     * @param idVer -
     * @param modelProvider -
     */
    public void updateResourceModel(String idVer, LwM2mModelProvider modelProvider) {
        Set<String> key = getKeysEqualsIdVer(idVer);
        key.forEach(k -> this.saveResourceModel(k, modelProvider));
    }

    private void saveResourceModel(String pathRez, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(convertToObjectIdFromIdVer(pathRez));
        ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
        this.resources.get(pathRez).setResourceModel(resourceModel);
    }

    private Set<String> getKeysEqualsIdVer(String idVer) {
        return this.resources.keySet()
                .stream()
                .filter(e -> idVer.equals(e.split(LWM2M_SEPARATOR_PATH)[1]))
                .collect(Collectors.toSet());
    }

    public void initValue(LwM2mTransportServiceImpl serviceImpl, String path) {
        if (path != null) {
            this.pendingRequests.remove(path);
        }
        if (this.pendingRequests.size() == 0) {
            this.init = true;
            serviceImpl.putDelayedUpdateResourcesThingsboard(this);
        }
    }

    public LwM2mClient copy() {
        return new LwM2mClient(this.endpoint, this.identity, this.securityInfo, this.credentialsResponse, this.profileId, this.sessionId);
    }
}

