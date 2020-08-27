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

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportService;
import org.thingsboard.server.transport.lwm2m.server.adaptors.ReadResultAttrTel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Data
public class ModelClient  implements Cloneable {
    private String endPoint;
    private String identity;
    private SecurityInfo info;
    private LwM2MTransportService transportService;
    private TransportProtos.ValidateDeviceCredentialsResponseMsg credentialsResponse;
    private Map<String, String> attributes;
    private Map<Integer, ModelObject> modelObjects;
    private Set<String> pendingRequests;
    private Map<String, LwM2mResponse> responses;
    private LeshanServer lwServer;
    private Registration registration;
    private JsonObject clientObserveAttrTelemetry;
    private ReadResultAttrTel readResultAttrTel;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    public ModelClient(String endPoint, String identity, SecurityInfo info, TransportProtos.ValidateDeviceCredentialsResponseMsg credentialsResponse, Map<String, String> attributes, Map<Integer, ModelObject> modelObjects) {
        this.endPoint = endPoint;
        this.identity = identity;
        this.info = info;
        this.credentialsResponse = credentialsResponse;
        this.attributes = (attributes != null && attributes.size()>0) ? attributes : new ConcurrentHashMap<String, String>();
        this.modelObjects =  (modelObjects != null && modelObjects.size()>0) ? modelObjects : new ConcurrentHashMap<Integer, ModelObject>();
        this.pendingRequests = ConcurrentHashMap.newKeySet();
        this.readResultAttrTel = new ReadResultAttrTel();
        /**
         * Key <objectId>, response<Value -> instance -> resources: value...>
         */
        this.responses = new ConcurrentHashMap<>();
    }

    /**
     *  Fill with data -> Model client
     * @param path
     * @param response
     */
    public void onSuccessHandler (String path, LwM2mResponse response) {
        this.responses.put(path, response);
        this.pendingRequests.remove(path);
        if (this.pendingRequests.size() == 0) {
            this.initValue ();
            this.transportService.getAttrTelemetryObserveFromModel(this.lwServer, this.registration, this);
        }
    }

    public void setRegistrationParam(LeshanServer lwServer, Registration registration) {
        this.lwServer = lwServer;
        this.registration = registration;
    }

    public void addPendingRequests(String path) {
        this.pendingRequests.add(path);
    }

    private void initValue () {
        this.responses.forEach((key, resp) -> {
            int objectId  = Integer.valueOf(key.split("/")[1]);
            ObjectModel objectModel = ((Collection<ObjectModel>)lwServer.getModelProvider().getObjectModel(registration).getObjectModels()).stream().filter(v -> v.id==objectId).collect(Collectors.toList()).get(0);
            if (this.modelObjects.get(objectId) != null) {
                this.modelObjects.get(objectId).getInstances().put(((ReadResponse) resp).getContent().getId(), (LwM2mObjectInstance)((ReadResponse) resp).getContent());
            }
            else {
                Map<Integer, LwM2mObjectInstance> instances = new ConcurrentHashMap<>();
                instances.put(((ReadResponse) resp).getContent().getId(), (LwM2mObjectInstance)((ReadResponse) resp).getContent());
                ModelObject modelObject =   new ModelObject(objectModel, instances);
                this.modelObjects.put(objectId, modelObject);
            }

//            instances.put(resp.getC)

//            ((LwM2mObject) ((ReadResponse) resp).getContent()).getInstances().entrySet().forEach(instance -> {
//                instances.put(instance.getKey(), instance.getValue());
//            });

        });
    }

}
