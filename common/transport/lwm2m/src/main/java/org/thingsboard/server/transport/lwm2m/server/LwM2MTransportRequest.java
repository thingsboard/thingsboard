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
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.transport.lwm2m.server.client.ModelClient;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import static org.eclipse.leshan.core.attributes.Attribute.*;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.*;

@Slf4j
@Service("LwM2MTransportRequest")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportRequest {
    private final ExecutorService executorService;
    private static final String RESPONSE_CHANNEL = "THINGSBOARD_RESP";
//    private final TransportService transportService;

    @Autowired
    LwM2MTransportService service;

    public LwM2MTransportRequest() {
//        this.transportService = service.context.getTransportService();
        executorService = Executors.newCachedThreadPool(
                new NamedThreadFactory(String.format("LwM2M %s channel response", RESPONSE_CHANNEL)));
    }


    @PostConstruct
    public void init() {
    }

    public Collection<Registration> doGetRegistrations(LeshanServer lwServer) {
        Collection<Registration> registrations = new ArrayList<>();
        for (Iterator<Registration> iterator = lwServer.getRegistrationService().getAllRegistrations(); iterator
                .hasNext(); ) {
            registrations.add(iterator.next());
        }
        return registrations;
    }

    /**
     * Device management and service enablement, including Read, Write, Execute, Discover, Create, Delete and Write-Attributes
     *
     * @param lwServer
     * @param registration
     * @param target
     * @param typeOper
     * @param contentFormatParam
     * @param modelClient
     * @param observation
     */
    @SneakyThrows
    public void sendAllRequest(LeshanServer lwServer, Registration registration, String target, String typeOper,
                               String contentFormatParam, ModelClient modelClient, Observation observation, String params, long timeoutInMs) {
        ResultIds resultIds = new ResultIds (target);
        if (registration != null && resultIds.getObjectId() >= 0) {
            DownlinkRequest request = null;
            ContentFormat contentFormat = contentFormatParam != null ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;
            timeoutInMs = timeoutInMs > 0 ? timeoutInMs : DEFAULT_TIMEOUT;
            switch (typeOper) {
                case GET_TYPE_OPER_READ:
                    request = new ReadRequest(contentFormat, target);
                    break;
                case GET_TYPE_OPER_DISCOVER:
                    request = new DiscoverRequest(target);
                    break;
                case GET_TYPE_OPER_OBSERVE:
                    if (resultIds.getResourceId() >= 0) {
                        request = new ObserveRequest(resultIds.getObjectId(), resultIds.getInstanceId(), resultIds.getResourceId());
                    } else if (resultIds.getInstanceId() >= 0) {
                        request = new ObserveRequest(resultIds.getObjectId(), resultIds.getInstanceId());
                    } else if (resultIds.getObjectId() >= 0) {
                        request = new ObserveRequest(resultIds.getObjectId());
                    }
                    break;
                case POST_TYPE_OPER_OBSERVE_CANCEL:
                    request = new CancelObservationRequest(observation);
                    break;
                case POST_TYPE_OPER_EXECUTE:
                    request = new ExecuteRequest(target, params);
                    break;
                case PUT_TYPE_OPER_UPDATE:
                    request = new ExecuteRequest(target, params);
                    break;
                case PUT_TYPE_OPER_WRITE:
                    if (resultIds.getResourceId() >= 0) {
                        ResourceModel resourceModel = lwServer.getModelProvider().getObjectModel(registration).getObjectModel(resultIds.getObjectId()).resources.get(resultIds.getResourceId());
                        ResourceModel.Type typeRes = resourceModel.type;
                        request = getWriteRequestResource(resultIds.getObjectId(), resultIds.getInstanceId(), resultIds.getResourceId(), params, typeRes);
                    }
                    break;
                case PUT_TYPE_OPER_WRITE_ATTRIBUTES:
                    /**
                     * As example:
                     * a)Write-Attributes/3/0/9?pmin=1 means the Battery Level value will be notified
                     * to the Server with a minimum interval of 1sec;
                     * this value is set at theResource level.
                     * b)Write-Attributes/3/0/9?pmin means the Battery Level will be notified
                     * to the Server with a minimum value (pmin) given by the default one
                     * (resource 2 of Object Server ID=1),
                     * or with another value if this Attribute has been set at another level
                     * (Object or Object Instance: see section5.1.1).
                     * c)Write-Attributes/3/0?pmin=10 means that all Resources of Instance 0 of the Object ‘Device (ID:3)’
                     * will be notified to the Server with a minimum interval of 10 sec;
                     * this value is set at the Object Instance level.
                     * d)Write-Attributes /3/0/9?gt=45&st=10 means the Battery Level will be notified to the Server
                     * when:
                     * a.old value is 20 and new value is 35 due to step condition
                     * b.old value is 45 and new value is 50 due to gt condition
                     * c.old value is 50 and new value is 40 due to both gt and step conditions
                     * d.old value is 35 and new value is 20 due to step conditione)
                     * Write-Attributes /3/0/9?lt=20&gt=85&st=10 means the Battery Level will be notified to the Server
                     * when:
                     * a.old value is 17 and new value is 24 due to lt condition
                     * b.old value is 75 and new value is 90 due to both gt and step conditions
                     * WriteAttributesResponse [code=INTERNAL_SERVER_ERROR, errormessage=not implemented]
                     * --> leshan-client-core/src/main/java/org/eclipse/leshan/client/resource/BaseObjectEnabler.java:
                     *      writeAttributes(ServerIdentity identity, WriteAttributesRequest request) строка: 344
                     */
//                    String uriQueries = "pmin=10&pmax=60";
//                    AttributeSet attributes = AttributeSet.parse(uriQueries);
//                    WriteAttributesRequest request = new WriteAttributesRequest(target, attributes);
//                    Attribute gt = new Attribute(GREATER_THAN, Double.valueOf("45"));
//                    Attribute st = new Attribute(LESSER_THAN, Double.valueOf("10"));
//                    Attribute pmax = new Attribute(MAXIMUM_PERIOD, "60");
//                    Attribute [] attrs = {gt, st};
                    Attribute pmin = new Attribute(MINIMUM_PERIOD, Integer.toUnsignedLong(Integer.valueOf("1")));
                    Attribute[] attrs = {pmin};
                    AttributeSet attrSet = new AttributeSet(attrs);
                    if (resultIds.getResourceId() >= 0) {
                        request = new WriteAttributesRequest(resultIds.getObjectId(), resultIds.getInstanceId(), resultIds.getResourceId(), attrSet);
                    } else if (resultIds.getInstanceId() >= 0) {
                        request = new WriteAttributesRequest(resultIds.getObjectId(), resultIds.getInstanceId(), attrSet);
                    } else if (resultIds.getObjectId() >= 0) {
                        request = new WriteAttributesRequest(resultIds.getObjectId(), attrSet);
                    }

                    break;
                default:
            }
            if (request != null) sendRequest(lwServer, registration, request, modelClient, timeoutInMs);
        }
    }

    /**
     *
     * @param lwServer
     * @param registration
     * @param request
     * @param modelClient
     * @param timeoutInMs
     */
    @SneakyThrows
    private void sendRequest(LeshanServer lwServer, Registration registration, DownlinkRequest request, ModelClient modelClient, long timeoutInMs) {
        lwServer.send(registration, request, timeoutInMs, (ResponseCallback<?>) response -> {
            handleResponse(registration, request.getPath().toString(), response, modelClient);
        }, e -> {
            log.error("SendRequest: \nerror: {}", e.toString());

        });
    }

    private WriteRequest getWriteRequestResource(Integer objectId, Integer instanceId, Integer resourceId, String value, ResourceModel.Type type) {
        switch (type) {
            case STRING:    // String
                return new WriteRequest(objectId, instanceId, resourceId, value);
            case INTEGER:   // Long
                return new WriteRequest(objectId, instanceId, resourceId, Integer.toUnsignedLong(Integer.valueOf(value)));
            case OBJLNK:    // ObjectLink
                return new WriteRequest(objectId, instanceId, resourceId, ObjectLink.fromPath(value));
            case BOOLEAN:   // Boolean
                return new WriteRequest(objectId, instanceId, resourceId, Boolean.valueOf(value));
            case FLOAT:     // Double
                return new WriteRequest(objectId, instanceId, resourceId, Double.valueOf(value));
            case TIME:      // Date
                return new WriteRequest(objectId, instanceId, resourceId, new Date((Long) Integer.toUnsignedLong(Integer.valueOf(value))));
            case OPAQUE:    // byte[] value, base64
                return new WriteRequest(objectId, instanceId, resourceId, Hex.decodeHex(value.toCharArray()));
            default:
        }
        return null;
    }

    @SneakyThrows
    private void handleResponse(Registration registration, final String path, LwM2mResponse response, ModelClient modelClient) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (path.equals("/3/0")) {
                        log.info(path);
                    }
                    sendResponse(registration, path, response, modelClient);
                } catch (RuntimeException t) {
                    log.error("Unable to send response. \n endpoint: {} \n path: {}\n error: {}", registration.getEndpoint(), path, t.toString());
                }
            }
        });
    }

    /**
     * processing a response from a client
     * @param registration
     * @param path
     * @param response
     * @param modelClient
     */
    @SneakyThrows
    private void sendResponse(Registration registration, String path, LwM2mResponse response, ModelClient modelClient) {
        if (response instanceof ObserveResponse) {
            service.onObservationResponse(registration, path, (ReadResponse) response);
        } else if (response instanceof CancelObservationResponse) {
            log.info("2_Send: Path: {}\n CancelObservationResponse: {} ", path, response);
        }
        else if (response instanceof ReadResponse) {
            /**
             * Use only at the first start after registration
             * Fill with data -> Model client
             */
            if (modelClient != null) {
                if (modelClient.getPendingRequests().size() > 0) {
                    modelClient.onSuccessHandler(path, response);
                }
            }
            /**
             * Use after registration on request
             */
            else {
                log.info("2_Send: Path: {}\n ReadResponse: {} ", path, response);
                service.onObservationResponse(registration, path, (ReadResponse)response);
            }
        } else if (response instanceof DeleteResponse) {
            log.info("2_Send: Path: {}\n DeleteResponse: {} ", path, response);
        } else if (response instanceof DiscoverResponse) {
            log.info("2_Send: Path: {}\n DiscoverResponse: {} ", path, response);
        } else if (response instanceof ExecuteResponse) {
            log.info("2_Send: Path: {}\n ExecuteResponse: {} ", path, response);
        } else if (response instanceof WriteAttributesResponse) {
            log.info("2_Send: Path: {}\n WriteAttributesResponse: {} ", path, response);
        }
    }
}
