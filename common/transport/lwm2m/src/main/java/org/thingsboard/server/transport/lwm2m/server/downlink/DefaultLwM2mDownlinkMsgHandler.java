/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeModel;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CompositeDownlinkRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MCancelObserveCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MObserveCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.rpc.composite.RpcWriteCompositeRequest;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.DIMENSION;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.ENABLER_VERSION;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.EVALUATE_MINIMUM_PERIOD;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.GREATER_THAN;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.LESSER_THAN;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.MINIMUM_PERIOD;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.OBJECT_VERSION;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.SERVER_URI;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.SHORT_SERVER_ID;
import static org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes.STEP;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.thingsboard.server.common.transport.util.JsonUtils.isBase64;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertMultiResourceValuesFromRpcBody;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.createModelsDefault;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.getVerFromPathIdVerOrId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.validateVersionedId;

@Slf4j
@Service("lwM2mDownlinkMsgHandler")
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mDownlinkMsgHandler extends LwM2MExecutorAwareService implements LwM2mDownlinkMsgHandler {

    public LwM2mValueConverterImpl converter;

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final LwM2MTelemetryLogService logService;
    private final LwM2mClientContext clientContext;
    private final LwM2mVersionedModelProvider modelProvider;

    @PostConstruct
    public void init() {
        super.init();
        this.converter = LwM2mValueConverterImpl.getInstance();
    }

    @PreDestroy
    public void destroy() {
        log.trace("Destroying {}", getClass().getSimpleName());
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
        ReadRequest downlink = new ReadRequest(getRequestContentFormat(client, request.getVersionedId(), modelProvider), request.getObjectId());
        sendSimpleRequest(client, downlink, request.getTimeout(), callback);
    }

    @Override
    public void sendReadCompositeRequest(LwM2mClient client, TbLwM2MReadCompositeRequest request,
                                         DownlinkRequestCallback<ReadCompositeRequest, ReadCompositeResponse> callback) {
        try {
            ContentFormat compositeContentFormat = this.findFirstContentFormatForComposite(client.getClientSupportContentFormats());
            ReadCompositeRequest downlink = new ReadCompositeRequest(compositeContentFormat, compositeContentFormat, request.getObjectIds());
            sendCompositeRequest(client, downlink, this.config.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    /**
     * if resource in CompositeObservation is already registered - return BAD REQUEST
     */
    @Override
    public void sendObserveRequest(LwM2mClient client, TbLwM2MObserveRequest request, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback) {
        try {
            validateVersionedId(client, request);
            LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
            String resourceExisting = checkResourceSingleObservationForExisting(client, resultIds.toString());
            if (StringUtils.isNotBlank(resourceExisting)) {
                callback.onValidationError(request.toString(), resourceExisting);
            } else {
                ObserveRequest downlink;
                ContentFormat contentFormat = getReadRequestContentFormat(client, request, modelProvider);
                if (resultIds.isResourceInstance()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId(), resultIds.getResourceInstanceId());
                } else if (resultIds.isResource()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId());
                } else if (resultIds.isObjectInstance()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId());
                } else {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId());
                }
                log.info("[{}] Send observation: {}.", client.getEndpoint(), request.getVersionedId());
                sendSimpleRequest(client, downlink, request.getTimeout(), callback);
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendObserveAllRequest(LwM2mClient client, TbLwM2MObserveAllRequest request, DownlinkRequestCallback<TbLwM2MObserveAllRequest, Set<String>> callback) {
        Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
        Set<String> paths = new LinkedHashSet<>();
        observations.stream().forEach(observation -> {
            if (observation instanceof SingleObservation) {
                paths.add("SingleObservation:" + ((SingleObservation) observation).getPath().toString());
            } else {
                paths.add("CompositeObservation: " + ((CompositeObservation) observation).getPaths().toString());
            }

        });
        callback.onSuccess(request, paths);
    }

    /**
     * if resource (SingleObservation or in CompositeObservation) is already registered - return BAD REQUEST
     */
    @Override
    public void sendObserveCompositeRequest(LwM2mClient client, TbLwM2MObserveCompositeRequest request, DownlinkRequestCallback<ObserveCompositeRequest,
            ObserveCompositeResponse> callback) {
        try {
            ContentFormat compositeContentFormat = this.findFirstContentFormatForComposite(client.getClientSupportContentFormats());
            String resourceExisting = checkResourceForExistingComposite(client, request.getObjectIds());
            if (StringUtils.isNotBlank(resourceExisting)) {
                callback.onValidationError(request.toString(), resourceExisting);
            } else {
                ObserveCompositeRequest downlink = new ObserveCompositeRequest(compositeContentFormat, compositeContentFormat, request.getObjectIds());
                log.trace("[{}] Send ObserveComposite: {}.", client.getEndpoint(), request.getVersionedIds());
                sendCompositeRequest(client, downlink, this.config.getTimeout(), callback);
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendCancelObserveCompositeRequest(LwM2mClient client, TbLwM2MCancelObserveCompositeRequest request, DownlinkRequestCallback<TbLwM2MCancelObserveCompositeRequest, Integer> callback) {
        try {
            log.trace("[{}] Send CancelObserveComposite: {}.", client.getEndpoint(), request.getVersionedIds());
            int cnt = context.getServer().getObservationService().cancelCompositeObservations(client.getRegistration(), request.getObjectIds());
            if (cnt != 0) {
                callback.onSuccess(request, cnt);
            } else {
                callback.onValidationError(request.toString(), "Could not find active Observe Composite component with paths: " + Arrays.toString(request.getVersionedIds()));
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendDiscoverAllRequest(LwM2mClient client, TbLwM2MDiscoverAllRequest request, DownlinkRequestCallback<TbLwM2MDiscoverAllRequest, List<String>> callback) {
        callback.onSuccess(request, Arrays.stream(client.getRegistration().getSortedObjectLinks()).map(Link::toCoreLinkFormat).collect(Collectors.toList()));
    }

    @Override
    public void sendExecuteRequest(LwM2mClient client, TbLwM2MExecuteRequest request, DownlinkRequestCallback<ExecuteRequest, ExecuteResponse> callback) {
        try {
            validateVersionedId(client, request);
            LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(request.getVersionedId()));
            ResourceModel resourceModelExecute = client.getResourceModel(request.getVersionedId(), modelProvider);
            if (resourceModelExecute == null) {
                LwM2mModel model = createModelsDefault();
                if (pathIds.isResource()) {
                    resourceModelExecute = model.getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
                }
            }
            if (resourceModelExecute == null) {
                callback.onValidationError(request.toString(), "ResourceModel with " + request.getVersionedId() +
                        " is absent in system. Need ddd Lwm2m Model with id=" + pathIds.getObjectId() + " ver=" +
                        getVerFromPathIdVerOrId(request.getVersionedId()) + " to profile.");
            } else if (resourceModelExecute.operations.isExecutable()) {
                ExecuteRequest downlink;
                if (request.getParams() != null && !resourceModelExecute.multiple) {
                    downlink = new ExecuteRequest(request.getObjectId(), (String) this.converter.convertValue(request.getParams(),
                            resourceModelExecute.type, ResourceModel.Type.STRING, new LwM2mPath(request.getObjectId())));
                } else {
                    downlink = new ExecuteRequest(request.getObjectId());
                }
                sendSimpleRequest(client, downlink, request.getTimeout(), callback);
            } else {
                callback.onValidationError(request.toString(), "Resource with " + request.getVersionedId() + " is not executable.");
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendDeleteRequest(LwM2mClient client, TbLwM2MDeleteRequest request, DownlinkRequestCallback<DeleteRequest, DeleteResponse> callback) {
        try {
            validateVersionedId(client, request);
            sendSimpleRequest(client, new DeleteRequest(request.getObjectId()), request.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendCancelObserveRequest(LwM2mClient client, TbLwM2MCancelObserveRequest request, DownlinkRequestCallback<TbLwM2MCancelObserveRequest, Integer> callback) {
        try {
            log.trace("[{}] Send CancelObserve {}.", client.getEndpoint(), request.getVersionedId());
            int cnt = context.getServer().getObservationService().cancelObservations(client.getRegistration(), request.getObjectId());
            if (cnt != 0) {
                callback.onSuccess(request, cnt);
            } else {
                callback.onValidationError(request.toString(), "Could not find active Observe component with path: " + request.getVersionedId());
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    @Override
    public void sendCancelObserveAllRequest(LwM2mClient client, TbLwM2MCancelAllRequest request, DownlinkRequestCallback<TbLwM2MCancelAllRequest, Integer> callback) {
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration());
        callback.onSuccess(request, observeCancelCnt);
    }

    @Override
    public void sendDiscoverRequest(LwM2mClient client, TbLwM2MDiscoverRequest request, DownlinkRequestCallback<DiscoverRequest, DiscoverResponse> callback) {
        validateVersionedId(client, request);
        sendSimpleRequest(client, new DiscoverRequest(request.getObjectId()), request.getTimeout(), callback);
    }

    /**
     * Example # 1:
     * AttributeSet attributes = new AttributeSet(new Attribute(Attribute.MINIMUM_PERIOD, 10L),
     * new Attribute(Attribute.MAXIMUM_PERIOD, 100L));
     * WriteAttributesRequest requestTest = new WriteAttributesRequest(3, 0, 14, attributes);
     * sendSimpleRequest(client, requestTest, request.getTimeout(), callback);
     * <p>
     * Example # 2
     * Dimension and Object version are read only attributes.
     * addAttribute(attributes, DIMENSION, params.getDim(), dim -> dim >= 0 && dim <= 255);
     * addAttribute(attributes, OBJECT_VERSION, params.getVer(), StringUtils::isNotEmpty, Function.identity());
     */
    @Override
    public void sendWriteAttributesRequest(LwM2mClient client, TbLwM2MWriteAttributesRequest request, DownlinkRequestCallback<WriteAttributesRequest, WriteAttributesResponse> callback) {
        try {
            validateVersionedId(client, request);
            if (request.getAttributes() == null) {
                throw new IllegalArgumentException("Attributes to write are not specified!");
            }
            sendSimpleRequest(client, new WriteAttributesRequest(request.getObjectId(), getAttributesSet(request.getAttributes())), request.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    private LwM2mAttributeSet getAttributesSet(ObjectAttributes params) {
        List<LwM2mAttribute<?>> attributes = new LinkedList<>();
        /**
         * Only: AttributeClass.NOTIFICATION -> RW
         */
        addAttribute(attributes, MAXIMUM_PERIOD, params.getPmax());
        addAttribute(attributes, MINIMUM_PERIOD, params.getPmin());
        addAttribute(attributes, GREATER_THAN, params.getGt());
        addAttribute(attributes, LESSER_THAN, params.getLt());
        addAttribute(attributes, STEP, params.getSt());
        addAttribute(attributes, EVALUATE_MAXIMUM_PERIOD, params.getEpmax());
        addAttribute(attributes, EVALUATE_MINIMUM_PERIOD, params.getEpmin());
        /**
         * Only:  AttributeClass.PROPERTIES -> R
         */
        addAttribute(attributes, DIMENSION, params.getDim());                        // Attachment.RESOURCE
        addAttribute(attributes, SHORT_SERVER_ID, params.getSsid());              // Attachment.OBJECT_INSTANCE
        addAttribute(attributes, SERVER_URI, params.getUri());                          // Attachment.OBJECT_INSTANCE
        if (params.getLwm2m() != null) {
            addAttribute(attributes, ENABLER_VERSION, params.getLwm2m());   // attachment.ROOT
        }
        if (params.getVer() != null) {
            addAttribute(attributes, OBJECT_VERSION, params.getVer());          // Attachment.OBJECT
        }

        return new LwM2mAttributeSet(attributes);
    }

    @Override
    public void sendWriteReplaceRequest(LwM2mClient client, TbLwM2MWriteReplaceRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        if (resultIds.isResource() || resultIds.isResourceInstance()) {
            validateVersionedId(client, request);
            ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), modelProvider);
            if (resourceModelWrite != null) {
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                try {
                    WriteRequest downlink = null;
                    String msgError = "";
                    if (resourceModelWrite.multiple) {
                        try {
                            Object valueForMultiResource = request.getValue();
                            if (resultIds.isResourceInstance()) {
                                String valueStr =  isBase64(request.getValue().toString()) ? "\"" + request.getValue() + "\"" : request.getValue().toString();
                                String resourceInstance = "{" + resultIds.getResourceInstanceId() + "=" + valueStr + "}";
                                valueForMultiResource = JsonParser.parseString(resourceInstance);
                            }
                            Map<Integer, Object> value = convertMultiResourceValuesFromRpcBody(valueForMultiResource, resourceModelWrite.type, request.getObjectId());
                            downlink = new WriteRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId(),
                                    value, resourceModelWrite.type);
                        } catch (Exception e) {
                        }
                    }
                    if (downlink == null) {
                        try {
                            downlink = this.getWriteRequestSingleResource(resourceModelWrite.type, contentFormat,
                                    resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId(), request.getValue());
                        } catch (Exception e) {
                            msgError = "Resource id=" + resultIds.toString() + ", value = " + request.getValue() +
                                    ", class = " + request.getValue().getClass().getSimpleName() + ". Format value is bad. Value for this Single Resource must be " + resourceModelWrite.type + "!";
                        }
                    }
                    if (downlink != null) {
                        sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                    } else {
                        callback.onValidationError(toString(request), msgError);
                    }
                } catch (Exception e) {
                    callback.onError(toString(request), e);
                }
            } else {
                callback.onValidationError(toString(request), "Resource " + request.getVersionedId() + " is not configured in the device profile!");
            }
        } else {
            callback.onValidationError(toString(request), "Resource " + request.getVersionedId() + ". This operation can only be used for Resource or ResourceInstance!");
        }
    }

    @Override
    public void sendWriteCompositeRequest(LwM2mClient client, RpcWriteCompositeRequest rpcWriteCompositeRequest,
                                          DownlinkRequestCallback<WriteCompositeRequest, WriteCompositeResponse> callback) {
        try {
            ContentFormat compositeContentFormat = this.findFirstContentFormatForComposite(client.getClientSupportContentFormats());
            WriteCompositeRequest downlink = new WriteCompositeRequest(compositeContentFormat, rpcWriteCompositeRequest.getNodes());
            //TODO: replace config.getTimeout();
            sendWriteCompositeRequest(client, downlink, this.config.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(rpcWriteCompositeRequest.toString(), e.getMessage());
        } catch (Exception e) {
            callback.onError(toString(rpcWriteCompositeRequest), e);
        }
    }

    @Override
    public void sendWriteUpdateRequest(LwM2mClient client, TbLwM2MWriteUpdateRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        try {
            LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
            if (resultIds.isObjectInstance() || resultIds.isResource()) {
                validateVersionedId(client, request);
                WriteRequest downlink = null;
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                String msgError = "";
                if (resultIds.isObjectInstance()) {
                    /*
                     *  params = "{\"id\":0,\"value\":[{\"id\":14,\"value\":\"+5\"},{\"id\":15,\"value\":\"+9\"}]}"
                     *  int rscId = resultIds.getObjectInstanceId();
                     *  contentFormat – Format of the payload (TLV or JSON).
                     */
                    Collection<LwM2mResource> resources = client.getNewResourcesForInstance(request.getVersionedId(),
                            request.getValue(), modelProvider, this.converter);
                    if (resources.size() > 0) {
                        downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                                resultIds.getObjectInstanceId(), resources);
                    } else {
                        msgError = " No resources to update!";
                    }
                } else if (resultIds.isResource()) {
                    ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), modelProvider);
                    if (resourceModelWrite != null) {
                        if (resourceModelWrite.multiple) {
                            try {
                                Map<Integer, Object> value = convertMultiResourceValuesFromRpcBody(request.getValue(), resourceModelWrite.type, request.getObjectId());
                                downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                                        resultIds.getObjectInstanceId(), resultIds.getResourceId(),
                                        value, resourceModelWrite.type);
                            } catch (Exception e1) {
                                msgError = " Resource id=" + resultIds.toString() +
                                        ", class = " + request.getValue().getClass().getSimpleName() +
                                        ", value = " + request.getValue() + " is bad. " +
                                        "Value of Multi-Instance Resource must be in Json format!";
                            }
                        }
                    } else {
                        msgError = " Resource " + request.getVersionedId() + " is not configured in the device profile!";
                    }
                }
                if (downlink != null) {
                    sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                } else {
                    callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                            ". This operation can only be used for ObjectInstance or Multi-Instance Resource !" + msgError);
                }
            } else {
                callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                        ". This operation can only be used for ObjectInstance or Resource (multiple)");
            }
        } catch (Exception e) {
            callback.onValidationError(toString(request), e.getMessage());
        }
    }

    public void sendCreateRequest(LwM2mClient client, TbLwM2MCreateRequest request, DownlinkRequestCallback<CreateRequest, CreateResponse> callback) {
        validateVersionedId(client, request);
        CreateRequest downlink = null;
        LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        ObjectModel objectModel = client.getObjectModel(request.getVersionedId(), modelProvider);
        // POST /{Object ID}/{Object Instance ID} && Resources is Mandatory
        if (objectModel != null) {
            if (objectModel.multiple) {

                // LwM2M CBOR, SenML CBOR, SenML JSON, or TLV (see [LwM2M-CORE])
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                if (resultIds.isObject() || resultIds.isObjectInstance()) {
                    Collection<LwM2mResource> resources;
                    if (resultIds.isObject()) {
                        if (request.getValue() != null) {
                            resources = client.getNewResourcesForInstance(request.getVersionedId(), request.getValue(), modelProvider, this.converter);
                            downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), resources);
                        } else if (request.getNodes() != null && request.getNodes().size() > 0) {
                            Set<LwM2mObjectInstance> instances = ConcurrentHashMap.newKeySet();
                            request.getNodes().forEach((key, value) -> {
                                Collection<LwM2mResource> resourcesForInstance = client.getNewResourcesForInstance(request.getVersionedId(), value, modelProvider, this.converter);
                                LwM2mObjectInstance instance = new LwM2mObjectInstance(Integer.parseInt(key), resourcesForInstance);
                                instances.add(instance);
                            });
                            LwM2mObjectInstance[] instanceArrays = instances.toArray(new LwM2mObjectInstance[instances.size()]);
                            downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), instanceArrays);
                        }

                    } else {
                        resources = client.getNewResourcesForInstance(request.getVersionedId(), request.getValue(), modelProvider, this.converter);
                        LwM2mObjectInstance instance = new LwM2mObjectInstance(resultIds.getObjectInstanceId(), resources);
                        downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), instance);
                    }
                }
                if (downlink != null) {
                    sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                } else {
                    callback.onValidationError(toString(request), "Path " + request.getVersionedId() +
                            ". Object must be Multiple !");
                }
            } else {
                throw new IllegalArgumentException("Path " + request.getVersionedId() + ". Object must be Multiple !");
            }
        } else {
            callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                    " is not configured in the device profile!");
        }
    }

    private <R extends SimpleDownlinkRequest<T>, T extends LwM2mResponse> void sendSimpleRequest(LwM2mClient client, R request, long timeoutInMs, DownlinkRequestCallback<R, T> callback) {
        sendRequest(client, request, timeoutInMs, callback, r -> request.getPath().toString());
    }

    private <R extends CompositeDownlinkRequest<T>, T extends LwM2mResponse> void sendCompositeRequest(LwM2mClient client, R request, long timeoutInMs, DownlinkRequestCallback<R, T> callback) {
        sendRequest(client, request, timeoutInMs, callback, r -> request.getPaths().toString());
    }

    private <R extends DownlinkRequest<T>, T extends LwM2mResponse> void sendRequest(LwM2mClient client, R request, long timeoutInMs, DownlinkRequestCallback<R, T> callback, Function<R, String> pathToStringFunction) {
        if (!clientContext.isDownlinkAllowed(client)) {
            log.trace("[{}] ignore downlink request cause client is sleeping.", client.getEndpoint());
            return;
        }
        Registration registration = client.getRegistration();
        try {
            logService.log(client, String.format("[%s][%s] Sending request: %s to %s", registration.getId(), registration.getSocketAddress(), request.getClass().getSimpleName(), pathToStringFunction.apply(request)));
            if (!callback.onSent(request)) {
                return;
            }

            context.getServer().send(registration, request, timeoutInMs, response -> {
                executor.submit(() -> {
                    try {
                        callback.onSuccess(request, response);
                    } catch (Exception e) {
                        log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                    } finally {
                        clientContext.awake(client);
                    }
                });
            }, e -> handleDownlinkError(client, request, callback, e));
        } catch (Exception e) {
            handleDownlinkError(client, request, callback, e);
        }
    }

    private <R extends SimpleDownlinkRequest<T>, T extends LwM2mResponse> void sendWriteCompositeRequest(LwM2mClient client, WriteCompositeRequest request, long timeoutInMs, DownlinkRequestCallback<WriteCompositeRequest, WriteCompositeResponse> callback) {
        if (!clientContext.isDownlinkAllowed(client)) {
            log.trace("[{}] ignore downlink request cause client is sleeping.", client.getEndpoint());
            return;
        }
        Registration registration = client.getRegistration();
        try {
            logService.log(client, String.format("[%s][%s] Sending request: %s to %s", registration.getId(), registration.getSocketAddress(), request.getClass().getSimpleName(), request.getPaths()));
            context.getServer().send(registration, request, timeoutInMs, response -> {
                executor.submit(() -> {
                    try {
                        if (response.isSuccess()) {
                            callback.onSuccess(request, response);
                        } else {
                            callback.onValidationError(request.getNodes().values().toString(), response.getErrorMessage());
                        }
                    } catch (Exception e) {
                        log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                    } finally {
                        clientContext.awake(client);
                    }
                });
            }, e -> handleDownlinkError(client, request, callback, e));
        } catch (Exception e) {
            handleDownlinkError(client, request, callback, e);
        }
    }

    private <R extends DownlinkRequest<T>, T extends LwM2mResponse> void handleDownlinkError(LwM2mClient client, R request, DownlinkRequestCallback<R, T> callback, Exception e) {
        log.trace("[{}] Received downlink error: {}.", client.getEndpoint(), e);
        try {
            client.updateLastUplinkTime();
            executor.submit(() -> {
                if (e instanceof TimeoutException || e instanceof ClientSleepingException) {
                    log.trace("[{}] Received {}, client is probably sleeping", client.getEndpoint(), e.getClass().getSimpleName());
                    clientContext.asleep(client);
                } else {
                    log.trace("[{}] Received {}", client.getEndpoint(), e.getClass().getSimpleName());
                }
                callback.onError(toString(request), e);
            });
        } catch (RejectedExecutionException ree) {
            log.warn("[{}] Can not handle downlink error. Executor already down", client.getEndpoint(), ree);
        } catch (Exception exception) {
            log.warn("[{}] Can not handle downlink error", client.getEndpoint(), exception);
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

    private static <T> void addAttribute(List<LwM2mAttribute<?>> attributes, LwM2mAttributeModel<T> attribute, T value) {
        addAttribute(attributes, attribute, value, null, null);
    }

    private static <T> void addAttribute(List<LwM2mAttribute<?>> attributes, LwM2mAttributeModel<T> attribute, T value, Function<T, ?> converter) {
        addAttribute(attributes, attribute, value, null, converter);
    }

    private static <T> void addAttribute(List<LwM2mAttribute<?>> attributes, LwM2mAttributeModel<T> attributeName, T value, Predicate<T> filter, Function<T, ?> converter) {
        if (value != null && ((filter == null) || filter.test(value))) {
            T valueConvert = (T) converter != null ? (T) converter.apply(value) : value;
            attributes.add(new LwM2mAttribute<>(attributeName, valueConvert));
        }
    }

    private static <T extends HasContentFormat & HasVersionedId> ContentFormat getReadRequestContentFormat(LwM2mClient client, T request, LwM2mModelProvider modelProvider) {
        if (request.getRequestContentFormat().isPresent()) {
            return request.getRequestContentFormat().get();
        } else {
            return getRequestContentFormat(client, request.getVersionedId(), modelProvider);
        }
    }

    private static ContentFormat getWriteRequestContentFormat(LwM2mClient client, TbLwM2MDownlinkRequest request, LwM2mModelProvider modelProvider) {
        if (request instanceof TbLwM2MWriteReplaceRequest && ((TbLwM2MWriteReplaceRequest) request).getContentFormat() != null) {
            return ((TbLwM2MWriteReplaceRequest) request).getContentFormat();
        } else if (request instanceof TbLwM2MWriteUpdateRequest && ((TbLwM2MWriteUpdateRequest) request).getObjectContentFormat() != null) {
            return ((TbLwM2MWriteUpdateRequest) request).getObjectContentFormat();
        } else {
            String versionedId = null;
            if (request instanceof TbLwM2MWriteReplaceRequest) {
                versionedId = ((TbLwM2MWriteReplaceRequest) request).getVersionedId();
            } else if (request instanceof TbLwM2MWriteUpdateRequest) {
                versionedId = ((TbLwM2MWriteUpdateRequest) request).getVersionedId();
            } else if (request instanceof TbLwM2MCreateRequest) {
                versionedId = ((TbLwM2MCreateRequest) request).getVersionedId();
            }
            return getRequestContentFormat(client, versionedId, modelProvider);
        }
    }

    private static ContentFormat getRequestContentFormat(LwM2mClient client, String versionedId, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(versionedId));
        if (pathIds.isResourceInstance() || pathIds.isResource()) {
            ResourceModel resourceModel = client.getResourceModel(versionedId, modelProvider);
            if (resourceModel != null && !resourceModel.multiple) {
                ContentFormat[] desiredFormats;
                if (OBJLNK.equals(resourceModel.type)) {
                    desiredFormats = new ContentFormat[]{ContentFormat.LINK, ContentFormat.CBOR, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON};
                } else if (OPAQUE.equals(resourceModel.type)) {
                    desiredFormats = new ContentFormat[]{ContentFormat.OPAQUE, ContentFormat.CBOR, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON};
                } else {
                    desiredFormats = new ContentFormat[]{ContentFormat.CBOR, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON};
                }
                return findFirstContentFormatForComp(client.getClientSupportContentFormats(), client.getDefaultContentFormat(), desiredFormats);
            } else {
                return getContentFormatForComplex(client);
            }
        } else {
            return getContentFormatForComplex(client);
        }
    }

    private static ContentFormat getContentFormatForComplex(LwM2mClient client) {
        if (LwM2m.LwM2mVersion.V1_0.equals(client.getRegistration().getLwM2mVersion())) {
            return client.getDefaultContentFormat();
        } else if (LwM2m.LwM2mVersion.V1_1.equals(client.getRegistration().getLwM2mVersion())) {
            ContentFormat result = findFirstContentFormatForComp(client.getClientSupportContentFormats(), client.getDefaultContentFormat(), ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON, ContentFormat.TLV, ContentFormat.JSON);
            if (result != null) {
                return result;
            } else {
                throw new RuntimeException("The client does not support any of SenML CBOR, SenML JSON, TLV or JSON formats. Can't send complex requests. Try using singe-instance requests.");
            }
        } else {
            throw new RuntimeException("The version " + client.getRegistration().getLwM2mVersion() + " is not supported!");
        }
    }

    private <R> String toString(R request) {
        try {
            return request != null ? request.toString() : "";
        } catch (Exception e) {
            log.debug("Failed to convert request to string", e);
            return request.getClass().getSimpleName();
        }
    }

    private ContentFormat findFirstContentFormatForComposite(Set<ContentFormat> clientSupportContentFormats) {
        ContentFormat contentFormat = findFirstContentFormatForComp(clientSupportContentFormats, null, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON);
        if (contentFormat != null) {
            return contentFormat;
        } else {
            throw new RuntimeException("This device does not support Composite Operation");
        }
    }

    private static ContentFormat findFirstContentFormatForComp(Set<ContentFormat> clientSupportContentFormats, ContentFormat defaultValue, ContentFormat... desiredFormats) {
        List desiredFormatsList = Arrays.asList(desiredFormats);
        for (ContentFormat c : clientSupportContentFormats) {
            if (desiredFormatsList.contains(c)) {
                return c;
            }
        }
        return defaultValue;
    }

    /**
     * Check if at least one of the resource objectIds (Composite) in SingleObservation or CompositeObservation is already registered
     * @param objectIds
     * @return
     */
    private String checkResourceForExistingComposite(LwM2mClient client, String[] objectIds) {
        List<String> objectIdsList = Arrays.asList(objectIds);
        Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
        for (Observation observation : observations) {
            if (observation instanceof SingleObservation singleObs) {
                String idSingleOb = singleObs.getPath().toString();
                if (objectIdsList.contains(idSingleOb)) {
                    return "Resource [" + idSingleOb + "] is already registered as SingleObservation.";
                }
            } else if (observation instanceof CompositeObservation compObs) {
                String paths = compObs.getPaths().toString();
                for (String idCompOb : objectIds) {
                    if (paths.contains(idCompOb)) {
                        return "Resource [" + idCompOb + "] is already registered in CompositeObservation.";
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check if the resource SingleObservation is already registered in CompositeObservation
     * Check if the resource SingleObservation is already registered in SingleObservation and (not equals path
     * @param objectId
     * @return
     */
    private String checkResourceSingleObservationForExisting(LwM2mClient client, String objectId) {
        Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
        for (Observation observation : observations) {
            if (observation instanceof SingleObservation singleObs) {
                LwM2mPath pathSingleOb = singleObs.getPath();
                LwM2mPath pathObjectId = new LwM2mPath(objectId);
                if (!pathSingleOb.toString().equals(objectId)) {
                    List paths = Arrays.asList(pathSingleOb, pathObjectId);
                    try {
                        LwM2mPath.validateNotOverlapping(paths);
                    } catch (IllegalArgumentException e){
                        return "Resource [" + objectId + "] conflict with is already registered as SingleObservation [" + pathSingleOb + "].";
                    }
                }
            }
            else if (observation instanceof CompositeObservation compObs) {
                String paths = compObs.getPaths().toString();
                if (paths.contains(objectId)) {
                    return "Resource [" + objectId + "] is already registered in CompositeObservation.";
                }
            }
        }
        return null;
    }
}
