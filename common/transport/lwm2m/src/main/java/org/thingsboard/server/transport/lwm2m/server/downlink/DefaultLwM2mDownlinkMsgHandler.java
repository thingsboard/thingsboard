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
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.device.data.lwm2m.ObjectAttributes;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.attributes.Attribute.GREATER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.LESSER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.MINIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.STEP;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mDownlinkMsgHandler extends LwM2MExecutorAwareService implements LwM2mDownlinkMsgHandler {

    public LwM2mValueConverterImpl converter;

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final LwM2MTelemetryLogService logService;

    @PostConstruct
    public void init() {
        super.init();
        this.converter = LwM2mValueConverterImpl.getInstance();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    protected int getExecutorSize() {
        return config.getDownlinkPoolSize();
    }

    @Override
    protected String getExecutorName() {
        return "LwM2M Downlink";
    }

    @Override
    public void sendReadRequest(LwM2mClient client, TbLwM2MReadRequest request, DownlinkRequestCallback<ReadRequest, ReadResponse> callback) {
        validateVersionedId(client, request);
        ReadRequest downlink = new ReadRequest(getContentFormat(client, request), request.getObjectId());
        sendRequest(client, downlink, request.getTimeout(), callback);
    }

    @Override
    public void sendObserveRequest(LwM2mClient client, TbLwM2MObserveRequest request, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback) {
        validateVersionedId(client, request);
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
        } else {
            throw new IllegalArgumentException("Observation is already registered!");
        }
    }

    @Override
    public void sendObserveAllRequest(LwM2mClient client, TbLwM2MObserveAllRequest request, DownlinkRequestCallback<TbLwM2MObserveAllRequest, Set<String>> callback) {
        Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
        Set<String> paths = observations.stream().map(observation -> observation.getPath().toString()).collect(Collectors.toUnmodifiableSet());
        callback.onSuccess(request, paths);
    }

    @Override
    public void sendDiscoverAllRequest(LwM2mClient client, TbLwM2MDiscoverAllRequest request, DownlinkRequestCallback<TbLwM2MDiscoverAllRequest, List<Link>> callback) {
        callback.onSuccess(request, Arrays.asList(client.getRegistration().getSortedObjectLinks()));
    }

    @Override
    public void sendExecuteRequest(LwM2mClient client, TbLwM2MExecuteRequest request, DownlinkRequestCallback<ExecuteRequest, ExecuteResponse> callback) {
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
    public void sendDeleteRequest(LwM2mClient client, TbLwM2MDeleteRequest request, DownlinkRequestCallback<DeleteRequest, DeleteResponse> callback) {
        sendRequest(client, new DeleteRequest(request.getObjectId()), request.getTimeout(), callback);
    }

    @Override
    public void sendCancelObserveRequest(LwM2mClient client, TbLwM2MCancelObserveRequest request, DownlinkRequestCallback<TbLwM2MCancelObserveRequest, Integer> callback) {
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration(), request.getObjectId());
        callback.onSuccess(request, observeCancelCnt);
    }

    @Override
    public void sendCancelAllRequest(LwM2mClient client, TbLwM2MCancelAllRequest request, DownlinkRequestCallback<TbLwM2MCancelAllRequest, Integer> callback) {
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration());
        callback.onSuccess(request, observeCancelCnt);
    }

    @Override
    public void sendDiscoverRequest(LwM2mClient client, TbLwM2MDiscoverRequest request, DownlinkRequestCallback<DiscoverRequest, DiscoverResponse> callback) {
        validateVersionedId(client, request);
        sendRequest(client, new DiscoverRequest(request.getObjectId()), request.getTimeout(), callback);
    }

    @Override
    public void sendWriteAttributesRequest(LwM2mClient client, TbLwM2MWriteAttributesRequest request, DownlinkRequestCallback<WriteAttributesRequest, WriteAttributesResponse> callback) {
        validateVersionedId(client, request);
        if (request.getAttributes() == null) {
            throw new IllegalArgumentException("Attributes to write are not specified!");
        }
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

    @Override
    public void sendWriteReplaceRequest(LwM2mClient client, TbLwM2MWriteReplaceRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), this.config.getModelProvider());
        if (resourceModelWrite != null) {
            ContentFormat contentFormat = convertResourceModelTypeToContentFormat(client, resourceModelWrite.type);
            try {
                LwM2mPath path = new LwM2mPath(request.getObjectId());
                WriteRequest downlink = this.getWriteRequestSingleResource(resourceModelWrite.type, contentFormat,
                        path.getObjectId(), path.getObjectInstanceId(), path.getResourceId(), request.getValue());
                sendRequest(client, downlink, request.getTimeout(), callback);
            } catch (Exception e) {
                callback.onError(JacksonUtil.toString(request), e);
            }
        } else {
            callback.onValidationError(JacksonUtil.toString(request), "Resource " + request.getVersionedId() + " is not configured in the device profile!");
        }
    }

    @Override
    public void sendWriteUpdateRequest(LwM2mClient client, TbLwM2MWriteUpdateRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        if (resultIds.isResource()) {
            /*
             * send request: path = '/3/0' node == wM2mObjectInstance
             * with params == "\"resources\": {15: resource:{id:15. value:'+01'...}}
             **/
            Collection<LwM2mResource> resources = client.getNewResourceForInstance(request.getVersionedId(), request.getValue(), this.config.getModelProvider(), this.converter);
            ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), this.config.getModelProvider());
            ContentFormat contentFormat = request.getObjectContentFormat() != null ? request.getObjectContentFormat() : convertResourceModelTypeToContentFormat(client, resourceModelWrite.type);
            WriteRequest downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
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
                callback.onValidationError(JacksonUtil.toString(request), "No resources to update!");
            }
        } else {
            callback.onValidationError(JacksonUtil.toString(request), "Update of the root level object is not supported yet!");
        }
    }

    private <R extends SimpleDownlinkRequest<T>, T extends LwM2mResponse> void sendRequest(LwM2mClient client, R request, long timeoutInMs, DownlinkRequestCallback<R, T> callback) {
        Registration registration = client.getRegistration();
        try {
            logService.log(client, String.format("[%s][%s] Sending request: %s to %s", registration.getId(), registration.getSocketAddress(), request.getClass().getSimpleName(), request.getPath()));
            context.getServer().send(registration, request, timeoutInMs, response -> {
                executor.submit(() -> {
                    try {
                        callback.onSuccess(request, response);
                    } catch (Exception e) {
                        log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                    }
                });
            }, e -> {
                executor.submit(() -> {
                    callback.onError(JacksonUtil.toString(request), e);
                });
            });
        } catch (Exception e) {
            callback.onError(JacksonUtil.toString(request), e);
        }
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
    }

    private void validateVersionedId(LwM2mClient client, HasVersionedId request) {
        if (!client.isValidObjectVersion(request.getVersionedId())) {
            throw new IllegalArgumentException("Specified resource id is not configured in the device profile!");
        }
        if (request.getObjectId() == null) {
            throw new IllegalArgumentException("Specified object id is null!");
        }
    }

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
