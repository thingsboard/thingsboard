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

import com.google.gson.JsonSyntaxException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
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
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.eclipse.leshan.server.bootstrap.DefaultBootstrapHandler.DEFAULT_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.*;

@Slf4j
@Service("LwM2MTransportRequest")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportRequest {
    private final ExecutorService executorService;
    private static final String RESPONSE_CHANNEL = "THINGSBOARD_RESP";

    @Autowired
    private LwM2MTransportContextServer context;

    public LwM2MTransportRequest() {
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

    public void sendAllRequest (LeshanServer lwServer, Registration registration, String target, String typeOper, String contentFormatParam, ModelClient modelClient, Observation observation) {
        String[] objects = (target.split("/").length > 0) ? target.split("/") : null;
        if (registration != null && objects != null) {
            DownlinkRequest request = null;

            int objectId = (objects.length > 1) ? Integer.parseInt(objects[1]) : -1;
            int instanceId = (objects.length > 2) ? Integer.parseInt(objects[2]) : -1;
            int resoutceId = (objects.length > 3) ? Integer.parseInt(objects[2]) : -1;
            switch (typeOper) {
                case GET_TYPE_OPER_READ:
                    /** get content format */
                    ContentFormat contentFormat = contentFormatParam != null ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;
                    /** create & process request */
                    request = new ReadRequest(contentFormat, target);
                    break;
                case GET_TYPE_OPER_DISCOVER:

                    break;
                case GET_TYPE_OPER_OBSERVE:
                    if (objectId > 0) {
                        request = new ObserveRequest(objectId);
                    }
                    break;
                case POST_TYPE_OPER_OBSERVE_CANCEL:
                    request = new CancelObservationRequest(observation);
                    break;
                case POST_TYPE_OPER_EXECUTE:

                    break;
                case PUT_TYPE_OPER_UPDATE:

                    break;
                case PUT_TYPE_OPER_WRIGHT:

                    break;
                default:
            }
            if (request != null) sendRequest(lwServer, registration, request, modelClient);
        }
    }

    @SneakyThrows
    public LwM2mResponse doGet(LeshanServer lwServer, Registration registration, String target, String typeOper, String contentFormatParam) {
        /** all registered clients */

        if (registration != null) {
           if (typeOper.equals(GET_TYPE_OPER_DISCOVER)) {
                try {
                    /** create & process request */
                    DownlinkRequest request = new DiscoverRequest(target);
                    sendRequest(lwServer, registration, request, null);

                } catch (RuntimeException  e) {
                    log.error("EndPoint: get client/discover: with id [{}]: [{}]", registration.getEndpoint(), e.toString());
                }
            }
            else if (typeOper.equals(GET_TYPE_OPER_READ)){
               try {
                   /** get content format */
                   ContentFormat contentFormat = contentFormatParam != null ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;
                   /** create & process request */
                   DownlinkRequest request = new ReadRequest(contentFormat, target);
                   sendRequest(lwServer, registration, request, null);
                   return null;
//                   return lwServer.send(registration, request, context.getTimeout());

               } catch (RuntimeException e) {
                   log.error("EndPoint: get client/read: with id [{}]: [{}]", registration.getEndpoint(), e.toString());
               }
           }
        } else {
            log.warn("EndPoint: get: no registered client with id [{}]", registration.getEndpoint());
        }
        return null;
    }

    @SneakyThrows
    public LwM2mResponse doPost(LeshanServer lwServer, String clientEndpoint, String target, String typeOper, String contentFormatParam, String params) {
        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration != null) {
            /** Execute */
            if (typeOper.equals(POST_TYPE_OPER_EXECUTE)) {
                ExecuteRequest request = new ExecuteRequest(target, params);
                return lwServer.send(registration, request, context.getTimeout());
            }
        }
        return null;
    }

    @SneakyThrows
    public LwM2mResponse doPut(LeshanServer lwServer, String clientEndpoint, String target, String typeOper, String contentFormatParam, String params) {
        Registration registration = lwServer.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration != null) {
            /** Update */
            if (typeOper.equals(PUT_TYPE_OPER_UPDATE)) {
                ExecuteRequest request = new ExecuteRequest(target, params);
                return lwServer.send(registration, request, context.getTimeout());
            }
            /** Wright */
            else if (typeOper.equals(PUT_TYPE_OPER_WRIGHT)) {

            }
        }
        return null;
    }

    public LwM2mResponse doPutResource (LeshanServer lwServer, Registration registration, Integer objectId, Integer instanceId, Integer resourceId, String value) {
        LwM2mResponse writeResponse = null;
        try {
            ResourceModel resourceModel = lwServer.getModelProvider().getObjectModel(registration).getObjectModel(objectId).resources.get(resourceId);
            ResourceModel.Type typeRes = resourceModel.type;
            WriteRequest writeRequest = getWriteRequestResource(objectId, instanceId, resourceId, value, typeRes);
            writeResponse = lwServer.send(registration, writeRequest);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("[{}] [{}] Received endpoint writeResponse", registration.getEndpoint(), writeResponse);
        return writeResponse;
    }

    private WriteRequest getWriteRequestResource (Integer objectId, Integer instanceId, Integer resourceId, String value, ResourceModel.Type type) {
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


//    public LwM2mResponse sendRequestReturn(LeshanServer lwServer, Registration registration, DownlinkRequest request) {
//        final LwM2mResponse[] responseRez = new LwM2mResponse[1];
//        CountDownLatch respLatch = new CountDownLatch(1);
//        if (registration != null) {
//            log.info("2) getClientModelWithValue  start: \n observeResponse do");
//            lwServer.send(registration, request, (ResponseCallback<?>) response -> {
//                log.info("5) getObserve: \nresponse: {}", response);
//                responseRez[0] = response;
//                respLatch.countDown();
//            }, e -> {
//                log.error("6) getCObservationObjectsTest: \nssh lwm2mError observe response: {}", e.toString());
//                respLatch.countDown();
//            });
//            try {
//                log.info("8) getCObservationObjectsTest: \nrespLatch.await: {}", DEFAULT_TIMEOUT*10);
//                respLatch.await(DEFAULT_TIMEOUT*10, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException ex) {
//                log.error("7) getCObservationObjectsTest: \nssh lwm2mError observe response: {}", ex.toString());
//            }
//        }
//        return responseRez[0];
//    }

    private void sendRequest(LeshanServer lwServer, Registration registration, DownlinkRequest request, ModelClient modelClient) {
        lwServer.send(registration, request, (ResponseCallback<?>) response -> {
//            log.info("5) sendRequest: \nresponse: {}", response);
            handleResponse(registration.getEndpoint(), request.getPath().toString(), response, modelClient);
//            log.info("5) sendRequest: \nresponse: {}", response);
        }, e -> {
            log.error("6) sendRequest: \nerror: {}", e.toString());

        });
    }

    public LwM2mResponse sendRequestAsync(LeshanServer lwServer, Registration registration, DownlinkRequest request) {
        final LwM2mResponse[] responseRez = new LwM2mResponse[1];
        CountDownLatch respLatch = new CountDownLatch(1);
        if (registration != null) {
            log.info("2) sendRequestAsync  start: \n observeResponse do");
            Thread t = new Thread(() -> {
                lwServer.send(registration, request, (ResponseCallback<?>) response -> {
                    log.info("5) sendRequestAsync: \nresponse: {}", response);
                    responseRez[0] = response;
                    respLatch.countDown();
                }, e -> {
                    log.error("6) sendRequestAsync: \nerr: {}", e.toString());
                    respLatch.countDown();
                });
            });
            t.start();
            try {
                respLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("7) sendRequestAsync: \nrespLatch err: {}", e.toString());
            }
        }
        return responseRez[0];
    }

    private void handleResponse(String clientEndpoint, final String path, LwM2mResponse response, ModelClient modelClient) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
//                    log.info("8) executorService.submit: \nresponse: {}", response);
                    sendResponse(path, response, modelClient);
                } catch (RuntimeException t) {
                    log.error("Unable to send response. \n endpoint: {} \n path: {}\n error: {}", clientEndpoint, path, t.toString());
                }
            }
        });
    }

    private void sendResponse(String path, LwM2mResponse response, ModelClient modelClient) {
        if (response instanceof ObserveResponse) {
            if (modelClient != null) {
                String[] objects = path.split("/");
                if (objects.length > 1) {
                    Integer objectId = Integer.parseInt(objects[1]);
                    if ( modelClient.getPendingRequests().size() > 0) {
                        modelClient.onSuccessHandler(objectId, response);
                    }
                }
            }
        }
        else if (response  instanceof CancelObservationResponse) {
            log.info("2_Send: Patn: {}\n CancelObservationResponse: {} ", path, response);
        }
        else if (response  instanceof ReadResponse) {
            log.info("2_Send: Patn: {}\n ReadResponse: {} ", path, response);
        }
        else if (response  instanceof DeleteResponse) {
            log.info("2_Send: Patn: {}\n DeleteResponse: {} ", path, response);
        }
        else if (response  instanceof DiscoverResponse) {
            log.info("2_Send: Patn: {}\n DiscoverResponse: {} ", path, response);
        }
        else if (response  instanceof ExecuteResponse) {
            log.info("2_Send: Patn: {}\n ExecuteResponse: {} ", path, response);
        }
    }

//    public LwM2mResponse getObserve(LeshanServer lwServer, Registration registration, DownlinkRequest observeRequest ) {
//        final LwM2mResponse[] responseRez = new LwM2mResponse[1];
//        CountDownLatch respLatch = new CountDownLatch(1);
//        if (registration != null) {
//            log.info("2) getClientModelWithValue  start: \n observeResponse do");
//            lwServer.send(registration, observeRequest, (ResponseCallback<?>) response -> {
//                log.info("5) getObserve: \nresponse: {}", response);
//                responseRez[0] = response;
//                respLatch.countDown();
//            }, e -> {
//                log.error("6) getCObservationObjectsTest: \nssh lwm2mError observe response: {}", e.toString());
//                respLatch.countDown();
//            });
//            try {
//                respLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException ex) {
//                log.error("7) getCObservationObjectsTest: \nssh lwm2mError observe response: {}", ex.toString());
//            }
//        }
//        return responseRez[0];
//    }

//    private void handleResponse(String clientEndpoint, final LwM2mResponse response) {
//        executorService.submit(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    sendResponse(response);
//                } catch (RuntimeException t) {
//                    log.error("Unable to send response.", t);
//                }
//            }
//        });
//    }

}
