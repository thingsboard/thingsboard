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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.node.codec.CodecException;
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
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.data.lwm2m.ObjectAttributes;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.rpc.LwM2mClientRpcRequest;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.attributes.Attribute.GREATER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.LESSER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.MINIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.STEP;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.RESPONSE_REQUEST_CHANNEL;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mDownlinkMsgHandler implements LwM2mDownlinkMsgHandler {
    private ExecutorService responseRequestExecutor;

    public LwM2mValueConverterImpl converter;

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final LwM2mClientContext lwM2mClientContext;

    @PostConstruct
    public void init() {
        this.converter = LwM2mValueConverterImpl.getInstance();
        responseRequestExecutor = Executors.newFixedThreadPool(this.config.getResponsePoolSize(),
                new NamedThreadFactory(String.format("LwM2M %s channel response after request", RESPONSE_REQUEST_CHANNEL)));
    }

    @Override
    public void sendReadRequest(LwM2mClient client, TbLwM2MReadRequest request, DownlinkRequestCallback<ReadResponse> callback) {
        if (request.getObjectId() != null && client.isValidObjectVersion(request.getVersionedId())) {
            ReadRequest downlink = new ReadRequest(getContentFormat(client, request), request.getObjectId());
            sendRequest(client, downlink, request.getTimeout(), callback);
        }
    }

    @Override
    public void sendObserveRequest(LwM2mClient client, TbLwM2MObserveRequest request, DownlinkRequestCallback<ObserveResponse> callback) {
        if (request.getObjectId() != null && client.isValidObjectVersion(request.getVersionedId())) {
            LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
            Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
            if (observations.stream().noneMatch(observation -> observation.getPath().equals(resultIds))) {
                ObserveRequest downlink;
                ContentFormat contentFormat = getContentFormat(client, request);
                if (resultIds.isResource()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId());
                } else if (resultIds.isObjectInstance()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId());
                } else {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId());
                }
                log.info("[{}] Send observation: {}.", client.getEndpoint(), request.getVersionedId());
                sendRequest(client, downlink, request.getTimeout(), callback);
            }
        }
    }

    @Override
    public void sendObserveAllRequest(LwM2mClient client, TbLwM2MObserveAllRequest request, DownlinkRequestCallback<Set<String>> callback) {
        Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
        Set<String> paths = observations.stream().map(observation -> observation.getPath().toString()).collect(Collectors.toUnmodifiableSet());
        callback.onSuccess(paths);
    }

    @Override
    public void sendDiscoverAllRequest(LwM2mClient client, TbLwM2MDiscoverAllRequest request, DownlinkRequestCallback<Set<String>> callback) {
        Link[] objectLinks = client.getRegistration().getSortedObjectLinks();
        Set<String> paths = Arrays.stream(objectLinks).map(Link::toString).collect(Collectors.toUnmodifiableSet());
        callback.onSuccess(paths);
    }

    @Override
    public void sendExecuteRequest(LwM2mClient client, TbLwM2MExecuteRequest request, DownlinkRequestCallback<ExecuteResponse> callback) {
        ResourceModel resourceModelExecute = client.getResourceModel(request.getVersionedId(), this.config.getModelProvider());
        if (resourceModelExecute != null) {
            ExecuteRequest downlink;
            if (request.getParams() != null && !resourceModelExecute.multiple) {
                downlink = new ExecuteRequest(request.getVersionedId(), (String) this.converter.convertValue(request.getParams(), resourceModelExecute.type, ResourceModel.Type.STRING, new LwM2mPath(request.getObjectId())));
            } else {
                downlink = new ExecuteRequest(request.getVersionedId());
            }
            sendRequest(client, downlink, request.getTimeout(), callback);
        }
    }

    @Override
    public void sendDeleteRequest(LwM2mClient client, TbLwM2MDeleteRequest request, DownlinkRequestCallback<DeleteResponse> callback) {
        sendRequest(client, new DeleteRequest(request.getObjectId()), request.getTimeout(), callback);
    }

    @Override
    public void sendCancelObserveRequest(LwM2mClient client, TbLwM2MCancelObserveRequest request, DownlinkRequestCallback<Integer> callback) {
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration(), request.getObjectId());
        callback.onSuccess(observeCancelCnt);
    }

    @Override
    public void sendCancelAllRequest(LwM2mClient client, TbLwM2MCancelAllRequest request, DownlinkRequestCallback<Integer> callback) {
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration());
        callback.onSuccess(observeCancelCnt);
    }

    @Override
    public void sendDiscoverRequest(LwM2mClient client, TbLwM2MDiscoverRequest request, DownlinkRequestCallback<DiscoverResponse> callback) {
        if (request.getObjectId() != null && client.isValidObjectVersion(request.getVersionedId())) {
            sendRequest(client, new DiscoverRequest(request.getObjectId()), request.getTimeout(), callback);
        }
    }

    @Override
    public void sendWriteAttributesRequest(LwM2mClient client, TbLwM2MWriteAttributesRequest request, DownlinkRequestCallback<WriteAttributesResponse> callback) {
        if (request.getObjectId() != null && client.isValidObjectVersion(request.getVersionedId()) && request.getAttributes() != null) {
            ObjectAttributes params = request.getAttributes();
            List<Attribute> attributes = new LinkedList<>();
//            Dimension and Object version are read only attributes.
//            addAttribute(attributes, DIMENSION, params.getDim(), dim -> dim >= 0 && dim <= 255);
//            addAttribute(attributes, OBJECT_VERSION, params.getVer(), StringUtils::isNotEmpty, Function.identity());
            addAttribute(attributes, MAXIMUM_PERIOD, params.getPmax());
            addAttribute(attributes, MINIMUM_PERIOD, params.getPmin());
            addAttribute(attributes, GREATER_THAN, params.getGt());
            addAttribute(attributes, LESSER_THAN, params.getLt());
            addAttribute(attributes, STEP, params.getSt());
            AttributeSet attributeSet = new AttributeSet(attributes);
            sendRequest(client, new WriteAttributesRequest(request.getObjectId(), attributeSet), request.getTimeout(), callback);
        }
    }

    @Override
    public void sendWriteReplaceRequest(LwM2mClient client, TbLwM2MWriteReplaceRequest request, DownlinkRequestCallback<WriteResponse> callback) {
        ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), this.config.getModelProvider());
        if (resourceModelWrite != null) {
            ContentFormat contentFormat = convertResourceModelTypeToContentFormat(client, resourceModelWrite.type);
            try {
                LwM2mPath path = new LwM2mPath(request.getObjectId());
                WriteRequest downlink = this.getWriteRequestSingleResource(resourceModelWrite.type, contentFormat,
                        path.getObjectId(), path.getObjectInstanceId(), path.getResourceId(), request.getValue());
                sendRequest(client, downlink, request.getTimeout(), callback);
            } catch (Exception e) {
                callback.onError(e);
            }
        } else {
            //TODO: log validation error using callback.
        }
    }

    @Override
    public void sendWriteUpdateRequest(LwM2mClient client, TbLwM2MWriteUpdateRequest request, DownlinkRequestCallback<WriteResponse> callback) {
        LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        if (resultIds.isResource()) {
            /*
             * send request: path = '/3/0' node == wM2mObjectInstance
             * with params == "\"resources\": {15: resource:{id:15. value:'+01'...}}
             **/
            Collection<LwM2mResource> resources = client.getNewResourceForInstance(request.getVersionedId(), request.getValue(), this.config.getModelProvider(), this.converter);
            ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), this.config.getModelProvider());
            WriteRequest downlink = new WriteRequest(WriteRequest.Mode.UPDATE, convertResourceModelTypeToContentFormat(client, resourceModelWrite.type), resultIds.getObjectId(),
                    resultIds.getObjectInstanceId(), resources);
            sendRequest(client, downlink, request.getTimeout(), callback);
        } else if (resultIds.isObjectInstance()) {
            /*
             *  params = "{\"id\":0,\"resources\":[{\"id\":14,\"value\":\"+5\"},{\"id\":15,\"value\":\"+9\"}]}"
             *  int rscId = resultIds.getObjectInstanceId();
             *  contentFormat – Format of the payload (TLV or JSON).
             */
            Collection<LwM2mResource> resources = client.getNewResourcesForInstance(request.getVersionedId(), request.getValue(), this.config.getModelProvider(), this.converter);
            if (resources.size() > 0) {
                ContentFormat contentFormat = request.getObjectContentFormat() != null ? request.getObjectContentFormat() : client.getDefaultContentFormat();
                WriteRequest downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resources);
                sendRequest(client, downlink, request.getTimeout(), callback);
            } else {
                callback.onValidationError("No resources to update!");
            }
        } else {
            callback.onValidationError("Update of the root level object is not supported yet!");
        }
    }

//    public void sendAllRequest(LwM2mClient client, String targetIdVer, LwM2mTypeOper typeOper,
//                               ContentFormat contentFormat, Object params, long timeoutInMs, LwM2mClientRpcRequest lwm2mClientRpcRequest) {
//        Registration registration = client.getRegistration();
//        try {
//            String target = fromVersionedIdToObjectId(targetIdVer);
//            if (contentFormat == null) {
//                contentFormat = client.getDefaultContentFormat();
//            }
//            LwM2mPath resultIds = target != null ? new LwM2mPath(target) : null;
//            if (!OBSERVE_CANCEL.name().equals(typeOper.name()) && resultIds != null && registration != null && resultIds.getObjectId() >= 0) {
//                if (client.isValidObjectVersion(targetIdVer)) {
//                    timeoutInMs = timeoutInMs > 0 ? timeoutInMs : DEFAULT_TIMEOUT;
//                    SimpleDownlinkRequest request = createRequest(registration, client, typeOper, contentFormat, target,
//                            targetIdVer, resultIds, params, lwm2mClientRpcRequest);
//                    if (request != null) {
//                        try {
//                            this.sendRequest(client, request, timeoutInMs, lwm2mClientRpcRequest);
//                        } catch (ClientSleepingException e) {
//                            SimpleDownlinkRequest finalRequest = request;
//                            long finalTimeoutInMs = timeoutInMs;
//                            LwM2mClientRpcRequest finalRpcRequest = lwm2mClientRpcRequest;
//                            client.getQueuedRequests().add(() -> sendRequest(client, finalRequest, finalTimeoutInMs, finalRpcRequest));
//                        } catch (Exception e) {
//                            log.error("[{}] [{}] [{}] Failed to send downlink.", registration.getEndpoint(), targetIdVer, typeOper.name(), e);
//                        }
//                    } else if (WRITE_UPDATE.name().equals(typeOper.name())) {
//                        if (lwm2mClientRpcRequest != null) {
//                            String errorMsg = String.format("Path %s params is not valid", targetIdVer);
//                            handler.sentRpcResponse(lwm2mClientRpcRequest, BAD_REQUEST.getName(), errorMsg, LOG_LW2M_ERROR);
//                        }
//                    } else if (WRITE_REPLACE.name().equals(typeOper.name()) || EXECUTE.name().equals(typeOper.name())) {
//                        if (lwm2mClientRpcRequest != null) {
//                            String errorMsg = String.format("Path %s object model  is absent", targetIdVer);
//                            handler.sentRpcResponse(lwm2mClientRpcRequest, BAD_REQUEST.getName(), errorMsg, LOG_LW2M_ERROR);
//                        }
//                    } else if (!OBSERVE_CANCEL.name().equals(typeOper.name())) {
//                        log.error("[{}], [{}] - [{}] error SendRequest", registration.getEndpoint(), typeOper.name(), targetIdVer);
//                        if (lwm2mClientRpcRequest != null) {
//                            ResourceModel resourceModel = client.getResourceModel(targetIdVer, this.config.getModelProvider());
//                            String errorMsg = resourceModel == null ? String.format("Path %s not found in object version", targetIdVer) : "SendRequest - null";
//                            handler.sentRpcResponse(lwm2mClientRpcRequest, NOT_FOUND.getName(), errorMsg, LOG_LW2M_ERROR);
//                        }
//                    }
//                } else if (lwm2mClientRpcRequest != null) {
//                    String errorMsg = String.format("Path %s not found in object version", targetIdVer);
//                    handler.sentRpcResponse(lwm2mClientRpcRequest, NOT_FOUND.getName(), errorMsg, LOG_LW2M_ERROR);
//                }
//            } else {
//                switch (typeOper) {
//                    case OBSERVE_READ_ALL:
//                    case DISCOVER_ALL:
//                        Set<String> paths;
//                        if (OBSERVE_READ_ALL.name().equals(typeOper.name())) {
//                            Set<Observation> observations = context.getServer().getObservationService().getObservations(registration);
//                            paths = observations.stream().map(observation -> observation.getPath().toString()).collect(Collectors.toUnmodifiableSet());
//                        } else {
//                            assert registration != null;
//                            Link[] objectLinks = registration.getSortedObjectLinks();
//                            paths = Arrays.stream(objectLinks).map(Link::toString).collect(Collectors.toUnmodifiableSet());
//                        }
//                        String msg = String.format("%s: type operation %s paths - %s", LOG_LW2M_INFO,
//                                typeOper.name(), paths);
//                        this.handler.sendLogsToThingsboard(client, msg);
//                        if (lwm2mClientRpcRequest != null) {
//                            String valueMsg = String.format("Paths - %s", paths);
//                            handler.sentRpcResponse(lwm2mClientRpcRequest, CONTENT.name(), valueMsg, LOG_LW2M_VALUE);
//                        }
//                        break;
//                    case OBSERVE_CANCEL:
//                    case OBSERVE_CANCEL_ALL:
//                        int observeCancelCnt = 0;
//                        String observeCancelMsg = null;
//                        if (OBSERVE_CANCEL.name().equals(typeOper)) {
//                            observeCancelCnt = context.getServer().getObservationService().cancelObservations(registration, target);
//                            observeCancelMsg = String.format("%s: type operation %s paths: %s count: %d", LOG_LW2M_INFO,
//                                    OBSERVE_CANCEL.name(), target, observeCancelCnt);
//                        } else {
//                            observeCancelCnt = context.getServer().getObservationService().cancelObservations(registration);
//                            observeCancelMsg = String.format("%s: type operation %s paths: All  count: %d", LOG_LW2M_INFO,
//                                    OBSERVE_CANCEL.name(), observeCancelCnt);
//                        }
//                        this.afterObserveCancel(client, observeCancelCnt, observeCancelMsg, lwm2mClientRpcRequest);
//                        break;
//                    // lwm2mClientRpcRequest != null
//                    case FW_UPDATE:
//                        handler.getInfoFirmwareUpdate(client, lwm2mClientRpcRequest);
//                        break;
//                }
//            }
//        } catch (Exception e) {
//            String msg = String.format("%s: type operation %s  %s", LOG_LW2M_ERROR,
//                    typeOper.name(), e.getMessage());
//            handler.sendLogsToThingsboard(client, msg);
//            if (lwm2mClientRpcRequest != null) {
//                String errorMsg = String.format("Path %s type operation %s  %s", targetIdVer, typeOper.name(), e.getMessage());
//                handler.sentRpcResponse(lwm2mClientRpcRequest, NOT_FOUND.getName(), errorMsg, LOG_LW2M_ERROR);
//            }
//        }
//    }

    private <T extends LwM2mResponse> void sendRequest(LwM2mClient client, SimpleDownlinkRequest<T> request, long timeoutInMs, DownlinkRequestCallback<T> callback) {
        Registration registration = client.getRegistration();
        context.getServer().send(registration, request, timeoutInMs, response -> {
//            if (!client.isInit()) {
//                client.initReadValue(this.handler, convertPathFromObjectIdToIdVer(request.getPath().toString(), registration));
//            }
            responseRequestExecutor.submit(() -> {
                try {
                    callback.onSuccess(response);
                } catch (Exception e) {
                    log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                }
            });
//            if (CoAP.ResponseCode.isSuccess(((Response) response.getCoapResponse()).getCode())) {
//                this.handleResponse(client, request.getPath().toString(), response, request, rpcRequest);
//            } else {
//                String msg = String.format("%s: SendRequest %s: CoapCode - %s Lwm2m code - %d name - %s Resource path - %s", LOG_LW2M_ERROR, request.getClass().getName().toString(),
//                        ((Response) response.getCoapResponse()).getCode(), response.getCode().getCode(), response.getCode().getName(), request.getPath().toString());
//                handler.sendLogsToThingsboard(client, msg);
//                log.error("[{}] [{}], [{}] - [{}] [{}] error SendRequest", request.getClass().getName().toString(), registration.getEndpoint(),
//                        ((Response) response.getCoapResponse()).getCode(), response.getCode(), request.getPath().toString());
//                if (!client.isInit()) {
//                    client.initReadValue(this.handler, convertPathFromObjectIdToIdVer(request.getPath().toString(), registration));
//                }
//                /** Not Found */
//                if (rpcRequest != null) {
//                    handler.sentRpcResponse(rpcRequest, response.getCode().getName(), response.getErrorMessage(), LOG_LW2M_ERROR);
//                }
//                /** Not Found
//                 set setClient_fw_info... = empty
//                 **/
//                if (client.getFwUpdate() != null && client.getFwUpdate().isInfoFwSwUpdate()) {
//                    client.getFwUpdate().initReadValue(handler, this, request.getPath().toString());
//                }
//                if (client.getSwUpdate() != null && client.getSwUpdate().isInfoFwSwUpdate()) {
//                    client.getSwUpdate().initReadValue(handler, this, request.getPath().toString());
//                }
//                if (request.getPath().toString().equals(FW_PACKAGE_5_ID) || request.getPath().toString().equals(SW_PACKAGE_ID)) {
//                    this.afterWriteFwSWUpdateError(registration, request, response.getErrorMessage());
//                }
//                if (request.getPath().toString().equals(FW_UPDATE_ID) || request.getPath().toString().equals(SW_INSTALL_ID)) {
//                    this.afterExecuteFwSwUpdateError(registration, request, response.getErrorMessage());
//                }
//            }
        }, e -> {
            responseRequestExecutor.submit(() -> {
                callback.onError(e);
            });
//            /** version == null
//             set setClient_fw_info... = empty
//             **/
//            if (client.getFwUpdate() != null && client.getFwUpdate().isInfoFwSwUpdate()) {
//                client.getFwUpdate().initReadValue(handler, this, request.getPath().toString());
//            }
//            if (client.getSwUpdate() != null && client.getSwUpdate().isInfoFwSwUpdate()) {
//                client.getSwUpdate().initReadValue(handler, this, request.getPath().toString());
//            }
//            if (request.getPath().toString().equals(FW_PACKAGE_5_ID) || request.getPath().toString().equals(SW_PACKAGE_ID)) {
//                this.afterWriteFwSWUpdateError(registration, request, e.getMessage());
//            }
//            if (request.getPath().toString().equals(FW_UPDATE_ID) || request.getPath().toString().equals(SW_INSTALL_ID)) {
//                this.afterExecuteFwSwUpdateError(registration, request, e.getMessage());
//            }
//            if (!client.isInit()) {
//                client.initReadValue(this.handler, convertPathFromObjectIdToIdVer(request.getPath().toString(), registration));
//            }
//            String msg = String.format("%s: SendRequest %s: Resource path - %s msg error - %s",
//                    LOG_LW2M_ERROR, request.getClass().getName().toString(), request.getPath().toString(), e.getMessage());
//            handler.sendLogsToThingsboard(client, msg);
//            log.error("[{}] [{}] - [{}] error SendRequest", request.getClass().getName().toString(), request.getPath().toString(), e.toString());
//            if (rpcRequest != null) {
//                handler.sentRpcResponse(rpcRequest, CoAP.CodeClass.ERROR_RESPONSE.name(), e.getMessage(), LOG_LW2M_ERROR);
//            }
        });
    }

    private WriteRequest getWriteRequestSingleResource(ResourceModel.Type type, ContentFormat contentFormat, int objectId, int instanceId, int resourceId, Object value) {
        switch (type) {
            case STRING:    // String
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, value.toString());
            case INTEGER:   // Long
                final long valueInt = Integer.toUnsignedLong(Integer.parseInt(value.toString()));
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueInt);
            case OBJLNK:    // ObjectLink
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, ObjectLink.fromPath(value.toString()));
            case BOOLEAN:   // Boolean
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, Boolean.parseBoolean(value.toString()));
            case FLOAT:     // Double
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, Double.parseDouble(value.toString()));
            case TIME:      // Date
                Date date = new Date(Long.decode(value.toString()));
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, date);
            case OPAQUE:    // byte[] value, base64
                byte[] valueRequest;
                if (value instanceof byte[]) {
                    valueRequest = (byte[]) value;
                } else {
                    valueRequest = Hex.decodeHex(value.toString().toCharArray());
                }
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueRequest);
            default:
                throw new IllegalArgumentException("Not supported type:" + type.name());
        }

//            TODO: throw exception and execute callback.
////            if (rpcRequest != null) {
////                String patn = "/" + objectId + "/" + instanceId + "/" + resourceId;
////                String errorMsg = String.format("Bad ResourceModel Operations (E): Resource path - %s ResourceModel type - %s", patn, type);
////                rpcRequest.setErrorMsg(errorMsg);
////            }
//            return null;
//        } catch (NumberFormatException e) {
//            String patn = "/" + objectId + "/" + instanceId + "/" + resourceId;
//            String msg = String.format(LOG_LW2M_ERROR + ": NumberFormatException: Resource path - %s type - %s value - %s msg error - %s  SendRequest to Client",
//                    patn, type, value, e.toString());
//            handler.sendLogsToThingsboard(client, msg);
//            log.error("Path: [{}] type: [{}] value: [{}] errorMsg: [{}]]", patn, type, value, e.toString());
//            if (rpcRequest != null) {
//                String errorMsg = String.format("NumberFormatException: Resource path - %s type - %s value - %s", patn, type, value);
//                handler.sentRpcResponse(rpcRequest, BAD_REQUEST.getName(), errorMsg, LOG_LW2M_ERROR);
//            }
//            return null;
//        }
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
     * @param path     -
     * @param response -
     */
    private void sendResponse(LwM2mClient lwM2mClient, String path, LwM2mResponse response,
                              SimpleDownlinkRequest request, LwM2mClientRpcRequest rpcRequest) {
//        Registration registration = lwM2mClient.getRegistration();
//        String pathIdVer = convertPathFromObjectIdToIdVer(path, registration);
//        String msgLog = "";
//        if (response instanceof ReadResponse) {
//            handler.onUpdateValueAfterReadResponse(registration, pathIdVer, (ReadResponse) response, rpcRequest);
//        } else if (response instanceof DeleteResponse) {
//            log.warn("11) [{}] Path [{}] DeleteResponse", pathIdVer, response);
//            if (rpcRequest != null) {
//                rpcRequest.setInfoMsg(null);
//                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), null, null);
//            }
//        } else if (response instanceof DiscoverResponse) {
//            String discoverValue = Link.serialize(((DiscoverResponse) response).getObjectLinks());
//            msgLog = String.format("%s: type operation: %s path: %s value: %s",
//                    LOG_LW2M_INFO, DISCOVER.name(), request.getPath().toString(), discoverValue);
//            handler.sendLogsToThingsboard(lwM2mClient, msgLog);
//            log.warn("DiscoverResponse: [{}]", (DiscoverResponse) response);
//            if (rpcRequest != null) {
//                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), discoverValue, LOG_LW2M_VALUE);
//            }
//        } else if (response instanceof ExecuteResponse) {
//            msgLog = String.format("%s: type operation: %s path: %s",
//                    LOG_LW2M_INFO, EXECUTE.name(), request.getPath().toString());
//            log.warn("9) [{}] ", msgLog);
//            handler.sendLogsToThingsboard(lwM2mClient, msgLog);
//            if (rpcRequest != null) {
//                msgLog = String.format("Start %s path: %S. Preparation finished: %s", EXECUTE.name(), path, rpcRequest.getInfoMsg());
//                rpcRequest.setInfoMsg(msgLog);
//                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), path, LOG_LW2M_INFO);
//            }
//
//        } else if (response instanceof WriteAttributesResponse) {
//            msgLog = String.format("%s: type operation: %s path: %s value: %s",
//                    LOG_LW2M_INFO, WRITE_ATTRIBUTES.name(), request.getPath().toString(), ((WriteAttributesRequest) request).getAttributes().toString());
//            handler.sendLogsToThingsboard(lwM2mClient, msgLog);
//            log.warn("12) [{}] Path [{}] WriteAttributesResponse", pathIdVer, response);
//            if (rpcRequest != null) {
//                handler.sentRpcResponse(rpcRequest, response.getCode().getName(), response.toString(), LOG_LW2M_VALUE);
//            }
//        } else if (response instanceof WriteResponse) {
//            msgLog = String.format("Type operation: Write path: %s", pathIdVer);
//            log.warn("10) [{}] response: [{}]", msgLog, response);
//            this.infoWriteResponse(lwM2mClient, response, request, rpcRequest);
//            handler.onWriteResponseOk(registration, pathIdVer, (WriteRequest) request);
//        }
    }

//    private void infoWriteResponse(LwM2mClient lwM2mClient, LwM2mResponse response, SimpleDownlinkRequest
//            request, LwM2mClientRpcRequest rpcRequest) {
//        try {
//            Registration registration = lwM2mClient.getRegistration();
//            LwM2mNode node = ((WriteRequest) request).getNode();
//            String msg = null;
//            Object value;
//            if (node instanceof LwM2mObject) {
//                msg = String.format("%s: Update finished successfully: Lwm2m code - %d Source path: %s  value: %s",
//                        LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), ((LwM2mObject) node).toString());
//            } else if (node instanceof LwM2mObjectInstance) {
//                msg = String.format("%s: Update finished successfully: Lwm2m code - %d Source path: %s  value: %s",
//                        LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), ((LwM2mObjectInstance) node).prettyPrint());
//            } else if (node instanceof LwM2mSingleResource) {
//                LwM2mSingleResource singleResource = (LwM2mSingleResource) node;
//                if (singleResource.getType() == ResourceModel.Type.STRING || singleResource.getType() == ResourceModel.Type.OPAQUE) {
//                    int valueLength;
//                    if (singleResource.getType() == ResourceModel.Type.STRING) {
//                        valueLength = ((String) singleResource.getValue()).length();
//                        value = ((String) singleResource.getValue())
//                                .substring(Math.min(valueLength, config.getLogMaxLength())).trim();
//
//                    } else {
//                        valueLength = ((byte[]) singleResource.getValue()).length;
//                        value = new String(Arrays.copyOf(((byte[]) singleResource.getValue()),
//                                Math.min(valueLength, config.getLogMaxLength()))).trim();
//                    }
//                    value = valueLength > config.getLogMaxLength() ? value + "..." : value;
//                    msg = String.format("%s: Update finished successfully: Lwm2m code - %d Resource path: %s length: %s value: %s",
//                            LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), valueLength, value);
//                } else {
//                    value = this.converter.convertValue(singleResource.getValue(),
//                            singleResource.getType(), ResourceModel.Type.STRING, request.getPath());
//                    msg = String.format("%s: Update finished successfully. Lwm2m code: %d Resource path: %s value: %s",
//                            LOG_LW2M_INFO, response.getCode().getCode(), request.getPath().toString(), value);
//                }
//            }
//            if (msg != null) {
//                handler.sendLogsToThingsboard(lwM2mClient, msg);
//                if (request.getPath().toString().equals(FW_PACKAGE_5_ID) || request.getPath().toString().equals(SW_PACKAGE_ID)) {
//                    this.afterWriteSuccessFwSwUpdate(registration, request);
//                    if (rpcRequest != null) {
//                        rpcRequest.setInfoMsg(msg);
//                    }
//                } else if (rpcRequest != null) {
//                    handler.sentRpcResponse(rpcRequest, response.getCode().getName(), msg, LOG_LW2M_INFO);
//                }
//            }
//        } catch (Exception e) {
//            log.trace("Fail convert value from request to string. ", e);
//        }
//    }

    /**
     * After finish operation FwSwUpdate Write (success):
     * fw_state/sw_state = DOWNLOADED
     * send operation Execute
     */
//    private void afterWriteSuccessFwSwUpdate(Registration registration, SimpleDownlinkRequest request) {
//        LwM2mClient client = this.lwM2mClientContext.getClientByRegistrationId(registration.getId());
//        if (request.getPath().toString().equals(FW_PACKAGE_5_ID) && client.getFwUpdate() != null) {
//            client.getFwUpdate().setStateUpdate(DOWNLOADED.name());
//            client.getFwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_INFO, null);
//        }
//        if (request.getPath().toString().equals(SW_PACKAGE_ID) && client.getSwUpdate() != null) {
//            client.getSwUpdate().setStateUpdate(DOWNLOADED.name());
//            client.getSwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_INFO, null);
//        }
//    }

    /**
     * After finish operation FwSwUpdate Write (error):  fw_state = FAILED
     */
//    private void afterWriteFwSWUpdateError(Registration registration, SimpleDownlinkRequest request, String
//            msgError) {
//        LwM2mClient client = this.lwM2mClientContext.getClientByRegistrationId(registration.getId());
//        if (request.getPath().toString().equals(FW_PACKAGE_5_ID) && client.getFwUpdate() != null) {
//            client.getFwUpdate().setStateUpdate(FAILED.name());
//            client.getFwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_ERROR, msgError);
//        }
//        if (request.getPath().toString().equals(SW_PACKAGE_ID) && client.getSwUpdate() != null) {
//            client.getSwUpdate().setStateUpdate(FAILED.name());
//            client.getSwUpdate().sendLogs(this.handler, WRITE_REPLACE.name(), LOG_LW2M_ERROR, msgError);
//        }
//    }

//    private void afterExecuteFwSwUpdateError(Registration registration, SimpleDownlinkRequest request, String
//            msgError) {
//        LwM2mClient client = this.lwM2mClientContext.getClientByRegistrationId(registration.getId());
//        if (request.getPath().toString().equals(FW_UPDATE_ID) && client.getFwUpdate() != null) {
//            client.getFwUpdate().sendLogs(this.handler, EXECUTE.name(), LOG_LW2M_ERROR, msgError);
//        }
//        if (request.getPath().toString().equals(SW_INSTALL_ID) && client.getSwUpdate() != null) {
//            client.getSwUpdate().sendLogs(this.handler, EXECUTE.name(), LOG_LW2M_ERROR, msgError);
//        }
//    }

//    private void afterObserveCancel(LwM2mClient lwM2mClient, int observeCancelCnt, String
//            observeCancelMsg, LwM2mClientRpcRequest rpcRequest) {
//        handler.sendLogsToThingsboard(lwM2mClient, observeCancelMsg);
//        log.warn("[{}]", observeCancelMsg);
//        if (rpcRequest != null) {
//            rpcRequest.setInfoMsg(String.format("Count: %d", observeCancelCnt));
//            handler.sentRpcResponse(rpcRequest, CONTENT.name(), null, LOG_LW2M_INFO);
//        }
//    }


    private static <T> void addAttribute(List<Attribute> attributes, String attributeName, T value) {
        addAttribute(attributes, attributeName, value, null, null);
    }

    private static <T> void addAttribute(List<Attribute> attributes, String attributeName, T value, Function<T, ?> converter) {
        addAttribute(attributes, attributeName, value, null, converter);
    }

    private static <T> void addAttribute(List<Attribute> attributes, String attributeName, T value, Predicate<T> filter, Function<T, ?> converter) {
        if (value != null && ((filter == null) || filter.test(value))) {
            attributes.add(new Attribute(attributeName, converter != null ? converter.apply(value) : value));
        }
    }

    private static ContentFormat convertResourceModelTypeToContentFormat(LwM2mClient client, ResourceModel.Type type) {
        switch (type) {
            case BOOLEAN:
            case STRING:
            case TIME:
            case INTEGER:
            case FLOAT:
                return client.getDefaultContentFormat();
            case OPAQUE:
                return ContentFormat.OPAQUE;
            case OBJLNK:
                return ContentFormat.LINK;
            default:
        }
        throw new CodecException("Invalid ResourceModel_Type for %s ContentFormat.", type);
    }

    private static ContentFormat getContentFormat(LwM2mClient client, HasContentFormat request) {
        return request.getContentFormat() != null ? request.getContentFormat() : client.getDefaultContentFormat();
    }
}
