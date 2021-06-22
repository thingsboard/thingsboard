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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientRpcRequest;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;
import static org.eclipse.leshan.core.ResponseCode.BAD_REQUEST;
import static org.eclipse.leshan.core.ResponseCode.NOT_FOUND;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper.getContentFormatByResourceModelType;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.DEFAULT_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_PACKAGE_5_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_UPDATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_VALUE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.DISCOVER;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.EXECUTE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE_CANCEL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE_READ_ALL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_ATTRIBUTES;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_REPLACE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_UPDATE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.RESPONSE_REQUEST_CHANNEL;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_INSTALL_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_PACKAGE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromIdVerToObjectId;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromObjectIdToIdVer;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.createWriteAttributeRequest;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mTransportRequest {
    private ExecutorService responseRequestExecutor;

    public LwM2mValueConverterImpl converter;

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final LwM2mClientContext lwM2mClientContext;
    private final DefaultLwM2MTransportMsgHandler handler;

    @PostConstruct
    public void init() {
        this.converter = LwM2mValueConverterImpl.getInstance();
        responseRequestExecutor = Executors.newFixedThreadPool(this.config.getResponsePoolSize(),
                new NamedThreadFactory(String.format("LwM2M %s channel response after request", RESPONSE_REQUEST_CHANNEL)));
    }

    public void sendAllRequest(LwM2mClient lwM2MClient, String targetIdVer, LwM2mTypeOper typeOper, Object params, long timeoutInMs, LwM2mClientRpcRequest lwm2mClientRpcRequest) {
        sendAllRequest(lwM2MClient, targetIdVer, typeOper, lwM2MClient.getDefaultContentFormat(), params, timeoutInMs, lwm2mClientRpcRequest);
    }


    public void sendAllRequest(LwM2mClient lwM2MClient, String targetIdVer, LwM2mTypeOper typeOper,
                               ContentFormat contentFormat, Object params, long timeoutInMs, LwM2mClientRpcRequest lwm2mClientRpcRequest) {
        Registration registration = lwM2MClient.getRegistration();
        try {
            String target = convertPathFromIdVerToObjectId(targetIdVer);
            if(contentFormat == null){
                contentFormat = ContentFormat.DEFAULT;
            }
            LwM2mPath resultIds = target != null ? new LwM2mPath(target) : null;
            if (!OBSERVE_CANCEL.name().equals(typeOper.name()) && resultIds != null && registration != null && resultIds.getObjectId() >= 0 && lwM2MClient != null) {
                if (lwM2MClient.isValidObjectVersion(targetIdVer)) {
                    timeoutInMs = timeoutInMs > 0 ? timeoutInMs : DEFAULT_TIMEOUT;
                    SimpleDownlinkRequest request = createRequest(registration, lwM2MClient, typeOper, contentFormat, target,
                            targetIdVer, resultIds, params, lwm2mClientRpcRequest);
                    if (request != null) {
                        try {
                            this.sendRequest(registration, lwM2MClient, request, timeoutInMs, lwm2mClientRpcRequest);
                        } catch (ClientSleepingException e) {
                            SimpleDownlinkRequest finalRequest = request;
                            long finalTimeoutInMs = timeoutInMs;
                            LwM2mClientRpcRequest finalRpcRequest = lwm2mClientRpcRequest;
                            lwM2MClient.getQueuedRequests().add(() -> sendRequest(registration, lwM2MClient, finalRequest, finalTimeoutInMs, finalRpcRequest));
                        } catch (Exception e) {
                            log.error("[{}] [{}] [{}] Failed to send downlink.", registration.getEndpoint(), targetIdVer, typeOper.name(), e);
                        }
                    } else if (WRITE_UPDATE.name().equals(typeOper.name())) {
                        if (lwm2mClientRpcRequest != null) {
                            String errorMsg = String.format("Path %s params is not valid", targetIdVer);
                            handler.sentRpcResponse(lwm2mClientRpcRequest, BAD_REQUEST.getName(), errorMsg, LOG_LW2M_ERROR);
                        }
                    } else if (WRITE_REPLACE.name().equals(typeOper.name()) || EXECUTE.name().equals(typeOper.name())) {
                        if (lwm2mClientRpcRequest != null) {
                            String errorMsg = String.format("Path %s object model  is absent", targetIdVer);
                            handler.sentRpcResponse(lwm2mClientRpcRequest, BAD_REQUEST.getName(), errorMsg, LOG_LW2M_ERROR);
                        }
                    } else if (!OBSERVE_CANCEL.name().equals(typeOper.name())) {
                        log.error("[{}], [{}] - [{}] error SendRequest", registration.getEndpoint(), typeOper.name(), targetIdVer);
                        if (lwm2mClientRpcRequest != null) {
                            ResourceModel resourceModel = lwM2MClient.getResourceModel(targetIdVer, this.config.getModelProvider());
                            String errorMsg = resourceModel == null ? String.format("Path %s not found in object version", targetIdVer) : "SendRequest - null";
                            handler.sentRpcResponse(lwm2mClientRpcRequest, NOT_FOUND.getName(), errorMsg, LOG_LW2M_ERROR);
                        }
                    }
                } else if (lwm2mClientRpcRequest != null) {
                    String errorMsg = String.format("Path %s not found in object version", targetIdVer);
                    handler.sentRpcResponse(lwm2mClientRpcRequest, NOT_FOUND.getName(), errorMsg, LOG_LW2M_ERROR);
                }
            } else {
                switch (typeOper) {
                    case OBSERVE_READ_ALL:
                    case DISCOVER_ALL:
                        Set<String> paths;
                        if (OBSERVE_READ_ALL.name().equals(typeOper.name())) {
                            Set<Observation> observations = context.getServer().getObservationService().getObservations(registration);
                            paths = observations.stream().map(observation -> observation.getPath().toString()).collect(Collectors.toUnmodifiableSet());
                        } else {
                            assert registration != null;
                            Link[] objectLinks = registration.getSortedObjectLinks();
                            paths = Arrays.stream(objectLinks).map(Link::toString).collect(Collectors.toUnmodifiableSet());
                        }
                        String msg = String.format("%s: type operation %s paths - %s", LOG_LW2M_INFO,
                                typeOper.name(), paths);
                        this.handler.sendLogsToThingsboard(lwM2MClient, msg);
                        if (lwm2mClientRpcRequest != null) {
                            String valueMsg = String.format("Paths - %s", paths);
                            handler.sentRpcResponse(lwm2mClientRpcRequest, CONTENT.name(), valueMsg, LOG_LW2M_VALUE);
                        }
                        break;
                    case OBSERVE_CANCEL:
                    case OBSERVE_CANCEL_ALL:
                        int observeCancelCnt = 0;
                        String observeCancelMsg = null;
                        if (OBSERVE_CANCEL.name().equals(typeOper)) {
                            observeCancelCnt = context.getServer().getObservationService().cancelObservations(registration, target);
                            observeCancelMsg = String.format("%s: type operation %s paths: %s count: %d", LOG_LW2M_INFO,
                                    OBSERVE_CANCEL.name(), target, observeCancelCnt);
                        } else {
                            observeCancelCnt = context.getServer().getObservationService().cancelObservations(registration);
                            observeCancelMsg = String.format("%s: type operation %s paths: All  count: %d", LOG_LW2M_INFO,
                                    OBSERVE_CANCEL.name(), observeCancelCnt);
                        }
                        this.afterObserveCancel(lwM2MClient, observeCancelCnt, observeCancelMsg, lwm2mClientRpcRequest);
                        break;
                    // lwm2mClientRpcRequest != null
                    case FW_UPDATE:
                        handler.getInfoFirmwareUpdate(lwM2MClient, lwm2mClientRpcRequest);
                        break;
                }
            }
        } catch (Exception e) {
            String msg = String.format("%s: type operation %s  %s", LOG_LW2M_ERROR,
                    typeOper.name(), e.getMessage());
            handler.sendLogsToThingsboard(lwM2MClient, msg);
            if (lwm2mClientRpcRequest != null) {
                String errorMsg = String.format("Path %s type operation %s  %s", targetIdVer, typeOper.name(), e.getMessage());
                handler.sentRpcResponse(lwm2mClientRpcRequest, NOT_FOUND.getName(), errorMsg, LOG_LW2M_ERROR);
            }
        }
    }

    private SimpleDownlinkRequest createRequest(Registration registration, LwM2mClient lwM2MClient, LwM2mTypeOper typeOper,
                                          ContentFormat contentFormat, String target, String targetIdVer,
                                          LwM2mPath resultIds, Object params, LwM2mClientRpcRequest rpcRequest) {
        SimpleDownlinkRequest request = null;
        switch (typeOper) {
            case READ:
                request = new ReadRequest(contentFormat, target);
                break;
            case DISCOVER:
                request = new DiscoverRequest(target);
                break;
            case OBSERVE:
                String msg = String.format("%s: Send Observation  %s.", LOG_LW2M_INFO, targetIdVer);
                log.warn(msg);
                if (resultIds.isResource()) {
                    Set<Observation> observations = context.getServer().getObservationService().getObservations(registration);
                    Set<Observation> paths = observations.stream().filter(observation -> observation.getPath().equals(resultIds)).collect(Collectors.toSet());
                    if (paths.size() == 0) {
                        request = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId());
                    } else {
                        request = new ReadRequest(contentFormat, target);
                    }
                } else if (resultIds.isObjectInstance()) {
                    request = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId());
                } else if (resultIds.getObjectId() >= 0) {
                    request = new ObserveRequest(contentFormat, resultIds.getObjectId());
                }
                break;
            case EXECUTE:
                ResourceModel resourceModelExecute = lwM2MClient.getResourceModel(targetIdVer, this.config.getModelProvider());
                if (resourceModelExecute != null) {
                    if (params != null && !resourceModelExecute.multiple) {
                        request = new ExecuteRequest(target, (String) this.converter.convertValue(params, resourceModelExecute.type, ResourceModel.Type.STRING, resultIds));
                    } else {
                        request = new ExecuteRequest(target);
                    }
                }
                break;
            case WRITE_REPLACE:
                /**
                 * Request to write a <b>String Single-Instance Resource</b> using the TLV content format.
                 * Type from resourceModel  -> STRING, INTEGER, FLOAT, BOOLEAN, OPAQUE, TIME, OBJLNK
                 * contentFormat ->           TLV,     TLV,    TLV,   TLV,     OPAQUE, TLV,  LINK
                 * JSON, TEXT;
                 **/
                ResourceModel resourceModelWrite = lwM2MClient.getResourceModel(targetIdVer, this.config.getModelProvider());
                if (resourceModelWrite != null) {
                    contentFormat = getContentFormatByResourceModelType(resourceModelWrite, contentFormat);
                    request = this.getWriteRequestSingleResource(contentFormat, resultIds.getObjectId(),
                            resultIds.getObjectInstanceId(), resultIds.getResourceId(), params, resourceModelWrite.type,
                            lwM2MClient, rpcRequest);
                }
                break;
            case WRITE_UPDATE:
                if (resultIds.isResource()) {
                    /**
                     * send request: path = '/3/0' node == wM2mObjectInstance
                     * with params == "\"resources\": {15: resource:{id:15. value:'+01'...}}
                     **/
                    Collection<LwM2mResource> resources = lwM2MClient.getNewResourceForInstance(
                            targetIdVer, params,
                            this.config.getModelProvider(),
                            this.converter);
                    contentFormat = getContentFormatByResourceModelType(lwM2MClient.getResourceModel(targetIdVer, this.config.getModelProvider()),
                            contentFormat);
                    request = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                            resultIds.getObjectInstanceId(), resources);
                }
                /**
                 *  params = "{\"id\":0,\"resources\":[{\"id\":14,\"value\":\"+5\"},{\"id\":15,\"value\":\"+9\"}]}"
                 *  int rscId = resultIds.getObjectInstanceId();
                 *  contentFormat – Format of the payload (TLV or JSON).
                 */
                else if (resultIds.isObjectInstance()) {
                    if (((ConcurrentHashMap) params).size() > 0) {
                        Collection<LwM2mResource> resources = lwM2MClient.getNewResourcesForInstance(
                                targetIdVer, params,
                                this.config.getModelProvider(),
                                this.converter);
                        if (resources.size() > 0) {
                            contentFormat = contentFormat.equals(ContentFormat.JSON) ? contentFormat : ContentFormat.TLV;
                            request = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                                    resultIds.getObjectInstanceId(), resources);
                        }
                    }
                } else if (resultIds.getObjectId() >= 0) {
                    request = new ObserveRequest(resultIds.getObjectId());
                }
                break;
            case WRITE_ATTRIBUTES:
                request = createWriteAttributeRequest(target, params, this.handler);
                break;
            case DELETE:
                request = new DeleteRequest(target);
                break;
        }
        return request;
    }

    /**
     * @param registration -
     * @param request      -
     * @param timeoutInMs  -
     */

    @SuppressWarnings({"error sendRequest"})
    private void sendRequest(Registration registration, LwM2mClient lwM2MClient, SimpleDownlinkRequest request,
                             long timeoutInMs, LwM2mClientRpcRequest rpcRequest) {
        context.getServer().send(registration, request, timeoutInMs, (ResponseCallback<?>) response -> {

            if (!lwM2MClient.isInit()) {
                lwM2MClient.initReadValue(this.handler, convertPathFromObjectIdToIdVer(request.getPath().toString(), registration));
            }
            if (CoAP.ResponseCode.isSuccess(((Response) response.getCoapResponse()).getCode())) {
                this.handleResponse(lwM2MClient, request.getPath().toString(), response, request, rpcRequest);
            } else {
                String msg = String.format("%s: SendRequest %s: CoapCode - %s Lwm2m code - %d name - %s Resource path - %s", LOG_LW2M_ERROR, request.getClass().getName().toString(),
                        ((Response) response.getCoapResponse()).getCode(), response.getCode().getCode(), response.getCode().getName(), request.getPath().toString());
                handler.sendLogsToThingsboard(lwM2MClient, msg);
                log.error("[{}] [{}], [{}] - [{}] [{}] error SendRequest", request.getClass().getName().toString(), registration.getEndpoint(),
                        ((Response) response.getCoapResponse()).getCode(), response.getCode(), request.getPath().toString());
                if (!lwM2MClient.isInit()) {
                    lwM2MClient.initReadValue(this.handler, convertPathFromObjectIdToIdVer(request.getPath().toString(), registration));
                }
                /** Not Found */
                if (rpcRequest != null) {
                    handler.sentRpcResponse(rpcRequest, response.getCode().getName(), response.getErrorMessage(), LOG_LW2M_ERROR);
                }
                /** Not Found
                 set setClient_fw_info... = empty
                 **/
                if (lwM2MClient.getFwUpdate() != null && lwM2MClient.getFwUpdate().isInfoFwSwUpdate()) {
                    lwM2MClient.getFwUpdate().initReadValue(handler, this, request.getPath().toString());
                }
                if (lwM2MClient.getSwUpdate() != null && lwM2MClient.getSwUpdate().isInfoFwSwUpdate()) {
                    lwM2MClient.getSwUpdate().initReadValue(handler, this, request.getPath().toString());
                }
                if (request.getPath().toString().equals(FW_PACKAGE_5_ID) || request.getPath().toString().equals(SW_PACKAGE_ID)) {
                    this.afterWriteFwSWUpdateError(registration, request, response.getErrorMessage());
                }
                if (request.getPath().toString().equals(FW_UPDATE_ID) || request.getPath().toString().equals(SW_INSTALL_ID)) {
                    this.afterExecuteFwSwUpdateError(registration, request, response.getErrorMessage());
                }
            }
        }, e -> {
            /** version == null
             set setClient_fw_info... = empty
             **/
            if (lwM2MClient.getFwUpdate() != null && lwM2MClient.getFwUpdate().isInfoFwSwUpdate()) {
                lwM2MClient.getFwUpdate().initReadValue(handler, this, request.getPath().toString());
            }
            if (lwM2MClient.getSwUpdate() != null && lwM2MClient.getSwUpdate().isInfoFwSwUpdate()) {
                lwM2MClient.getSwUpdate().initReadValue(handler, this, request.getPath().toString());
            }
            if (request.getPath().toString().equals(FW_PACKAGE_5_ID) || request.getPath().toString().equals(SW_PACKAGE_ID)) {
                this.afterWriteFwSWUpdateError(registration, request, e.getMessage());
            }
            if (request.getPath().toString().equals(FW_UPDATE_ID) || request.getPath().toString().equals(SW_INSTALL_ID)) {
                this.afterExecuteFwSwUpdateError(registration, request, e.getMessage());
            }
            if (!lwM2MClient.isInit()) {
                lwM2MClient.initReadValue(this.handler, convertPathFromObjectIdToIdVer(request.getPath().toString(), registration));
            }
            String msg = String.format("%s: SendRequest %s: Resource path - %s msg error - %s",
                    LOG_LW2M_ERROR, request.getClass().getName().toString(), request.getPath().toString(), e.getMessage());
            handler.sendLogsToThingsboard(lwM2MClient, msg);
            log.error("[{}] [{}] - [{}] error SendRequest", request.getClass().getName().toString(), request.getPath().toString(), e.toString());
            if (rpcRequest != null) {
                handler.sentRpcResponse(rpcRequest, CoAP.CodeClass.ERROR_RESPONSE.name(), e.getMessage(), LOG_LW2M_ERROR);
            }
        });
    }

    private WriteRequest getWriteRequestSingleResource(ContentFormat contentFormat, Integer objectId, Integer instanceId,
                                                       Integer resourceId, Object value, ResourceModel.Type type,
                                                       LwM2mClient client, LwM2mClientRpcRequest rpcRequest) {
        try {
            if (type != null) {
                switch (type) {
                    case STRING:    // String
                        return (contentFormat == null) ? new WriteRequest(objectId, instanceId, resourceId, value.toString()) : new WriteRequest(contentFormat, objectId, instanceId, resourceId, value.toString());
                    case INTEGER:   // Long
                        final long valueInt = Integer.toUnsignedLong(Integer.parseInt(value.toString()));
                        return (contentFormat == null) ? new WriteRequest(objectId, instanceId, resourceId, valueInt) : new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueInt);
                    case OBJLNK:    // ObjectLink
                        return (contentFormat == null) ? new WriteRequest(objectId, instanceId, resourceId, ObjectLink.fromPath(value.toString())) : new WriteRequest(contentFormat, objectId, instanceId, resourceId, ObjectLink.fromPath(value.toString()));
                    case BOOLEAN:   // Boolean
                        return (contentFormat == null) ? new WriteRequest(objectId, instanceId, resourceId, Boolean.parseBoolean(value.toString())) : new WriteRequest(contentFormat, objectId, instanceId, resourceId, Boolean.parseBoolean(value.toString()));
                    case FLOAT:     // Double
                        return (contentFormat == null) ? new WriteRequest(objectId, instanceId, resourceId, Double.parseDouble(value.toString())) : new WriteRequest(contentFormat, objectId, instanceId, resourceId, Double.parseDouble(value.toString()));
                    case TIME:      // Date
                        Date date = new Date(Long.decode(value.toString()));
                        return (contentFormat == null) ? new WriteRequest(objectId, instanceId, resourceId, date) : new WriteRequest(contentFormat, objectId, instanceId, resourceId, date);
                    case OPAQUE:    // byte[] value, base64
                        byte[] valueRequest = value instanceof byte[] ? (byte[]) value : Hex.decodeHex(value.toString().toCharArray());
                        return (contentFormat == null) ? new WriteRequest(objectId, instanceId, resourceId, valueRequest) :
                                new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueRequest);
                    default:
                }
            }
            if (rpcRequest != null) {
                String patn = "/" + objectId + "/" + instanceId + "/" + resourceId;
                String errorMsg = String.format("Bad ResourceModel Operations (E): Resource path - %s ResourceModel type - %s", patn, type);
                rpcRequest.setErrorMsg(errorMsg);
            }
            return null;
        } catch (NumberFormatException e) {
            String patn = "/" + objectId + "/" + instanceId + "/" + resourceId;
            String msg = String.format(LOG_LW2M_ERROR + ": NumberFormatException: Resource path - %s type - %s value - %s msg error - %s  SendRequest to Client",
                    patn, type, value, e.toString());
            handler.sendLogsToThingsboard(client, msg);
            log.error("Path: [{}] type: [{}] value: [{}] errorMsg: [{}]]", patn, type, value, e.toString());
            if (rpcRequest != null) {
                String errorMsg = String.format("NumberFormatException: Resource path - %s type - %s value - %s", patn, type, value);
                handler.sentRpcResponse(rpcRequest, BAD_REQUEST.getName(), errorMsg, LOG_LW2M_ERROR);
            }
            return null;
        }
    }

    private void handleResponse(LwM2mClient lwM2mClient, final String path, LwM2mResponse response,
                                SimpleDownlinkRequest request, LwM2mClientRpcRequest rpcRequest) {
        responseRequestExecutor.submit(() -> {
            try {
                this.sendResponse(lwM2mClient, path, response, request, rpcRequest);
            } catch (Exception e) {
                log.error("[{}] endpoint [{}] path [{}] Exception Unable to after send response.", lwM2mClient.getRegistration().getEndpoint(), path, e);
            }
        });
    }

    /**
     * processing a response from a client
     *
     * @param path         -
     * @param response     -
     */
    private void sendResponse(LwM2mClient lwM2mClient, String path, LwM2mResponse response,
                              SimpleDownlinkRequest request, LwM2mClientRpcRequest rpcRequest) {
        Registration registration = lwM2mClient.getRegistration();
        String pathIdVer = convertPathFromObjectIdToIdVer(path, registration);
        String msgLog = "";
        if (response instanceof ReadResponse) {
            handler.onUpdateValueAfterReadResponse(registration, pathIdVer, (ReadResponse) response, rpcRequest);
        } else if (response instanceof DeleteResponse) {
            log.warn("11) [{}] Path [{}] DeleteResponse", pathIdVer, response);
            if (rpcRequest != null) {
                rpcRequest.setInfoMsg(null);
                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), null, null);
            }
        } else if (response instanceof DiscoverResponse) {
            String discoverValue = Link.serialize(((DiscoverResponse) response).getObjectLinks());
            msgLog = String.format("%s: type operation: %s path: %s value: %s",
                    LOG_LW2M_INFO, DISCOVER.name(), request.getPath().toString(), discoverValue);
            handler.sendLogsToThingsboard(lwM2mClient, msgLog);
            log.warn("DiscoverResponse: [{}]", (DiscoverResponse) response);
            if (rpcRequest != null) {
                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), discoverValue, LOG_LW2M_VALUE);
            }
        } else if (response instanceof ExecuteResponse) {
            msgLog = String.format("%s: type operation: %s path: %s",
                    LOG_LW2M_INFO, EXECUTE.name(), request.getPath().toString());
            log.warn("9) [{}] ", msgLog);
            handler.sendLogsToThingsboard(lwM2mClient, msgLog);
            if (rpcRequest != null) {
                msgLog = String.format("Start %s path: %S. Preparation finished: %s", EXECUTE.name(), path, rpcRequest.getInfoMsg());
                rpcRequest.setInfoMsg(msgLog);
                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), path, LOG_LW2M_INFO);
            }

        } else if (response instanceof WriteAttributesResponse) {
            msgLog = String.format("%s: type operation: %s path: %s value: %s",
                    LOG_LW2M_INFO, WRITE_ATTRIBUTES.name(), request.getPath().toString(), ((WriteAttributesRequest) request).getAttributes().toString());
            handler.sendLogsToThingsboard(lwM2mClient, msgLog);
            log.warn("12) [{}] Path [{}] WriteAttributesResponse", pathIdVer, response);
            if (rpcRequest != null) {
                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), response.toString(), LOG_LW2M_VALUE);
            }
        } else if (response instanceof WriteResponse) {
            msgLog = String.format("Type operation: Write path: %s", pathIdVer);
            log.warn("10) [{}] response: [{}]", msgLog, response);
            this.infoWriteResponse(lwM2mClient, response, request, rpcRequest);
            handler.onWriteResponseOk(registration, pathIdVer, (WriteRequest) request);
        }
    }

    private void infoWriteResponse(LwM2mClient lwM2mClient, LwM2mResponse response, SimpleDownlinkRequest request, LwM2mClientRpcRequest rpcRequest) {
        try {
            Registration registration = lwM2mClient.getRegistration();
            LwM2mNode node = ((WriteRequest) request).getNode();
            String msg = null;
            Object value;
            if (node instanceof LwM2mObject) {
                msg = String.format("%s: Update finished successfully: Lwm2m code - %d Source path: %s  value: %s",
                        LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), ((LwM2mObject) node).toString());
            } else if (node instanceof LwM2mObjectInstance) {
                msg = String.format("%s: Update finished successfully: Lwm2m code - %d Source path: %s  value: %s",
                        LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), ((LwM2mObjectInstance) node).prettyPrint());
            } else if (node instanceof LwM2mSingleResource) {
                LwM2mSingleResource singleResource = (LwM2mSingleResource) node;
                if (singleResource.getType() == ResourceModel.Type.STRING || singleResource.getType() == ResourceModel.Type.OPAQUE) {
                    int valueLength;
                    if (singleResource.getType() == ResourceModel.Type.STRING) {
                        valueLength = ((String) singleResource.getValue()).length();
                        value = ((String) singleResource.getValue())
                                .substring(Math.min(valueLength, config.getLogMaxLength())).trim();

                    } else {
                        valueLength = ((byte[]) singleResource.getValue()).length;
                        value = new String(Arrays.copyOf(((byte[]) singleResource.getValue()),
                                Math.min(valueLength, config.getLogMaxLength()))).trim();
                    }
                    value = valueLength > config.getLogMaxLength() ? value + "..." : value;
                    msg = String.format("%s: Update finished successfully: Lwm2m code - %d Resource path: %s length: %s value: %s",
                            LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), valueLength, value);
                } else {
                    value = this.converter.convertValue(singleResource.getValue(),
                            singleResource.getType(), ResourceModel.Type.STRING, request.getPath());
                    msg = String.format("%s: Update finished successfully. Lwm2m code: %d Resource path: %s value: %s",
                            LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), value);
                }
            }
            if (msg != null) {
                handler.sendLogsToThingsboard(lwM2mClient, msg);
                if (request.getPath().toString().equals(FW_PACKAGE_5_ID) || request.getPath().toString().equals(SW_PACKAGE_ID)) {
                    this.afterWriteSuccessFwSwUpdate(registration, request);
                    if (rpcRequest != null) {
                        rpcRequest.setInfoMsg(msg);
                    }
                }
                else if (rpcRequest != null) {
                    handler.sentRpcResponse(rpcRequest, response.getCode().getName(), msg, LOG_LW2M_INFO);
                }
            }
        } catch (Exception e) {
            log.trace("Fail convert value from request to string. ", e);
        }
    }

    /**
     * After finish operation FwSwUpdate Write (success):
     * fw_state/sw_state = DOWNLOADED
     * send operation Execute
     */
    private void afterWriteSuccessFwSwUpdate(Registration registration, SimpleDownlinkRequest request) {
        LwM2mClient lwM2MClient = this.lwM2mClientContext.getClientByRegistrationId(registration.getId());
        if (request.getPath().toString().equals(FW_PACKAGE_5_ID) && lwM2MClient.getFwUpdate() != null) {
            lwM2MClient.getFwUpdate().setStateUpdate(DOWNLOADED.name());
            lwM2MClient.getFwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_INFO, null);
        }
        if (request.getPath().toString().equals(SW_PACKAGE_ID) && lwM2MClient.getSwUpdate() != null) {
            lwM2MClient.getSwUpdate().setStateUpdate(DOWNLOADED.name());
            lwM2MClient.getSwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_INFO, null);
        }
    }

    /**
     * After finish operation FwSwUpdate Write (error):  fw_state = FAILED
     */
    private void afterWriteFwSWUpdateError(Registration registration, SimpleDownlinkRequest request, String msgError) {
        LwM2mClient lwM2MClient = this.lwM2mClientContext.getClientByRegistrationId(registration.getId());
        if (request.getPath().toString().equals(FW_PACKAGE_5_ID) && lwM2MClient.getFwUpdate() != null) {
            lwM2MClient.getFwUpdate().setStateUpdate(FAILED.name());
            lwM2MClient.getFwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_ERROR, msgError);
        }
        if (request.getPath().toString().equals(SW_PACKAGE_ID) && lwM2MClient.getSwUpdate() != null) {
            lwM2MClient.getSwUpdate().setStateUpdate(FAILED.name());
            lwM2MClient.getSwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_ERROR, msgError);
        }
    }

    private void afterExecuteFwSwUpdateError(Registration registration, SimpleDownlinkRequest request, String msgError) {
        LwM2mClient lwM2MClient = this.lwM2mClientContext.getClientByRegistrationId(registration.getId());
        if (request.getPath().toString().equals(FW_UPDATE_ID) && lwM2MClient.getFwUpdate() != null) {
            lwM2MClient.getFwUpdate().sendLogs(this.handler, EXECUTE.name(), LOG_LW2M_ERROR, msgError);
        }
        if (request.getPath().toString().equals(SW_INSTALL_ID) && lwM2MClient.getSwUpdate() != null) {
            lwM2MClient.getSwUpdate().sendLogs(this.handler, EXECUTE.name(), LOG_LW2M_ERROR, msgError);
        }
    }

    private void afterObserveCancel(LwM2mClient lwM2mClient, int observeCancelCnt, String observeCancelMsg, LwM2mClientRpcRequest rpcRequest) {
        handler.sendLogsToThingsboard(lwM2mClient, observeCancelMsg);
        log.warn("[{}]", observeCancelMsg);
        if (rpcRequest != null) {
            rpcRequest.setInfoMsg(String.format("Count: %d", observeCancelCnt));
            handler.sentRpcResponse(rpcRequest, CONTENT.name(), null, LOG_LW2M_INFO);
        }
    }
}
