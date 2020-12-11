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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportService;
import org.thingsboard.server.transport.lwm2m.server.ResultIds;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;

@Slf4j
@Data
public class LwM2MClient implements Cloneable {
    private String deviceName;
    private String deviceProfileName;
    private String endPoint;
    private String identity;
    private SecurityInfo info;
    private UUID deviceUuid;
    private UUID sessionUuid;
    private UUID profileUuid;
    private LeshanServer lwServer;
    private LwM2MTransportService lwM2MTransportService;
    private Registration registration;
    private ValidateDeviceCredentialsResponseMsg credentialsResponse;
    private Map<String, String> attributes;
    private Map<Integer, ModelObject> modelObjects;
    private Set<String> pendingRequests;
    private Map<String, TransportProtos.TsKvProto> delayedRequests;
    private Set<Integer> delayedRequestsId;
    private Map<String, LwM2mResponse> responses;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public LwM2MClient(String endPoint, String identity, SecurityInfo info, ValidateDeviceCredentialsResponseMsg credentialsResponse, Map<String, String> attributes, Map<Integer, ModelObject> modelObjects, UUID profileUuid) {
        this.endPoint = endPoint;
        this.identity = identity;
        this.info = info;
        this.credentialsResponse = credentialsResponse;
        this.attributes = (attributes != null && attributes.size() > 0) ? attributes : new ConcurrentHashMap<String, String>();
        this.modelObjects = (modelObjects != null && modelObjects.size() > 0) ? modelObjects : new ConcurrentHashMap<Integer, ModelObject>();
        this.pendingRequests = ConcurrentHashMap.newKeySet();
        this.delayedRequests = new ConcurrentHashMap<>();
        this.delayedRequestsId = ConcurrentHashMap.newKeySet();
        this.profileUuid = profileUuid;
        /**
         * Key <objectId>, response<Value -> instance -> resources: value...>
         */
        this.responses = new ConcurrentHashMap<>();
    }

    /**
     *  Fill with data -> Model client
     * @param path -
     * @param response -
     */
    public void onSuccessHandler(String path, LwM2mResponse response) {
        this.responses.put(path, response);
        this.pendingRequests.remove(path);
        if (this.pendingRequests.size() == 0) {
            this.initValue();
            this.lwM2MTransportService.putDelayedUpdateResourcesThingsboard(this);
        }
    }

    private void initValue() {
        this.responses.forEach((key, resp) -> {
            ResultIds pathIds = new ResultIds(key);
            if (pathIds.getObjectId() > -1) {
                ObjectModel objectModel = ((Collection<ObjectModel>) this.lwServer.getModelProvider().getObjectModel(registration).getObjectModels()).stream().filter(v -> v.id == pathIds.getObjectId()).collect(Collectors.toList()).get(0);
                if (this.modelObjects.get(pathIds.getObjectId()) != null) {
                    this.modelObjects.get(pathIds.getObjectId()).getInstances().put(((ReadResponse) resp).getContent().getId(), (LwM2mObjectInstance) ((ReadResponse) resp).getContent());
                } else {
                    Map<Integer, LwM2mObjectInstance> instances = new ConcurrentHashMap<>();
                    instances.put(((ReadResponse) resp).getContent().getId(), (LwM2mObjectInstance) ((ReadResponse) resp).getContent());
                    ModelObject modelObject = new ModelObject(objectModel, instances);
                    this.modelObjects.put(pathIds.getObjectId(), modelObject);
                }
            }
        });
    }

    public void onSuccessDelayedRequests (String path) {
        if (path != null) this.delayedRequests.remove(path);
        if (this.delayedRequests.size() == 0 && this.getDelayedRequestsId().size() == 0) {
            this.lwM2MTransportService.updatesAndSentModelParameter(this);
        }
    }

    public ResourceModel.Operations getOperation(String path) {
        ResultIds resultIds = new ResultIds(path);
        return (this.getModelObjects().get(resultIds.getObjectId()) != null) ? this.getModelObjects().get(resultIds.getObjectId()).getObjectModel().resources.get(resultIds.getResourceId()).operations : ResourceModel.Operations.NONE;
    }

    public String getResourceName (String path) {
        ResultIds resultIds = new ResultIds(path);
        return (this.getModelObjects().get(resultIds.getObjectId()) != null) ? this.getModelObjects().get(resultIds.getObjectId()).getObjectModel().resources.get(resultIds.getResourceId()).name : "";
    }

    /**
     * @param path - path resource
     * @return - value of Resource or null
     */public String getResourceValue(String path) {
        String resValue = null;
        ResultIds pathIds = new ResultIds(path);
        ModelObject modelObject = this.getModelObjects().get(pathIds.getObjectId());
        if (modelObject != null && modelObject.getInstances().get(pathIds.getInstanceId()) != null) {
            LwM2mObjectInstance instance = modelObject.getInstances().get(pathIds.getInstanceId());
            if (instance.getResource(pathIds.getResourceId()) != null) {
                resValue = instance.getResource(pathIds.getResourceId()).getType() == OPAQUE ?
                        Hex.encodeHexString((byte[]) instance.getResource(pathIds.getResourceId()).getValue()).toLowerCase() :
                        (instance.getResource(pathIds.getResourceId()).isMultiInstances()) ?
                                instance.getResource(pathIds.getResourceId()).getValues().toString() :
                                instance.getResource(pathIds.getResourceId()).getValue().toString();
            }
        }
        return resValue;
    }
}

