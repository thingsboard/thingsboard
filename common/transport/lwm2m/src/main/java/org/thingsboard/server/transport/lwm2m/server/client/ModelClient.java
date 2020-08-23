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
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private Set<Integer> pendingRequests;
    private Map<Integer, LwM2mResponse> responses;
    private LeshanServer lwServer;
    private Registration registration;

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
        this.responses = new ConcurrentHashMap<>();
    }

    public void onSuccessHandler (Integer objectId, LwM2mResponse response) {
        this.responses.put(objectId, response);
        this.pendingRequests.remove(objectId);
        if (this.pendingRequests.size() == 0) {
            log.info("19) model: finish: objectId: {} \n this.pendingRequests.size() {}", objectId, this.pendingRequests.size());
            /**
             * Cancel All observation
             */
            int cancel = lwServer.getObservationService().cancelObservations(registration);
            Set<Observation> observations = lwServer.getObservationService().getObservations(registration);
            log.info("33_1)  setCancelObservationObjects endpoint: {} cancel: {}  observations: {}", registration.getEndpoint(), cancel, observations);
            initValue ();
            this.transportService.getAttrTelemetryObserveFromModel(this.lwServer, this.endPoint);
        }
    }

    public void setRegistrationParam(LeshanServer lwServer, Registration registration) {
        this.lwServer = lwServer;
        this.registration = registration;
    }

    public void addPendingRequests(Integer request) {
        this.pendingRequests.add(request);
    }
    private void initValue () {
        log.info("41)  initValue");
    }
}
