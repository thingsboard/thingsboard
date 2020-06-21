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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;


import javax.annotation.PostConstruct;
import java.util.*;


@Service("LwM2MTransportRequest")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MTransportRequest {

//    private ObjectModelSerDes serializer;
//    private Gson gson;
//    private JsonObject jsonModelAttributes;
//    private JsonObject jsonModelTelemetry;
//    @Autowired
//    private LwM2MTransportService service;

    @Autowired
    private LeshanServer lwServer;

    @Autowired
    private LwM2MTransportContext context;


    @PostConstruct
    public void init() {
    }

    public Collection<Registration> doGetRegistrations() {
        Collection<Registration> registrations = new ArrayList<>();
        for (Iterator<Registration> iterator = lwServer.getRegistrationService().getAllRegistrations(); iterator
                .hasNext(); ) {
            registrations.add(iterator.next());
        }
        return registrations;
    }

    @SneakyThrows
    public LwM2mResponse doGet(String clientEndpoint, String target, String typeOper, String contentFormatParam) {
        // all registered clients
        // lwM2MTransportRequest.doGet(null);
        LwM2mResponse cResponse = null;
        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration != null) {
           if (typeOper.equals("discover")) {
                try {
                    // create & process request
                    DiscoverRequest request = new DiscoverRequest(target);
                    return lwServer.send(registration, request, context.getTimeout());
                } catch (RuntimeException | InterruptedException e) {
                    log.error("EndPoint: get client/discover: with id [{}]: [{}]", clientEndpoint, e);
                }
            }
            else if (typeOper.equals("read")){
               try {
                   // get content format
                   ContentFormat contentFormat = contentFormatParam != null ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;
                   // create & process request
                   ReadRequest request = new ReadRequest(contentFormat, target);
                   return lwServer.send(registration, request, context.getTimeout());
               } catch (RuntimeException | InterruptedException e) {
                   log.error("EndPoint: get client/read: with id [{}]: [{}]", clientEndpoint, e);
               }
           }
        } else {
            log.warn("EndPoint: get: no registered client with id [{}]", clientEndpoint);
        }
        return null;
    }
}
