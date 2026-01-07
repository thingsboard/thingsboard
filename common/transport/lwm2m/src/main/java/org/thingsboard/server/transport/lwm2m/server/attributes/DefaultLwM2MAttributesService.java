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
package org.thingsboard.server.transport.lwm2m.server.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper.getValueFromKvProto;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_TAG;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_TITLE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_URL;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_VERSION;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_TAG;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_TITLE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_URL;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_VERSION;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_WARN;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.compareAttNameKeyOta;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertMultiResourceValuesFromJson;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.valueEquals;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MAttributesService implements LwM2MAttributesService {

    //TODO: add timeout logic
    private final AtomicInteger reqIdSeq = new AtomicInteger();
    private final Map<Integer, SettableFuture<List<TransportProtos.TsKvProto>>> futures;

    private final TransportService transportService;
    private final LwM2mTransportServerHelper helper;
    private final LwM2mClientContext clientContext;
    private final LwM2MTransportServerConfig config;
    private final LwM2mUplinkMsgHandler uplinkHandler;
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    private final LwM2MTelemetryLogService logService;
    private final LwM2MOtaUpdateService otaUpdateService;
    private final LwM2mModelProvider modelProvider;

    @Override
    public ListenableFuture<List<TransportProtos.TsKvProto>> getSharedAttributes(LwM2mClient client, Collection<String> keys) {
        SettableFuture<List<TransportProtos.TsKvProto>> future = SettableFuture.create();
        int requestId = reqIdSeq.incrementAndGet();
        futures.put(requestId, future);
        transportService.process(client.getSession(), TransportProtos.GetAttributeRequestMsg.newBuilder().setRequestId(requestId).
                addAllSharedAttributeNames(keys).build(), new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void msg) {

            }

            @Override
            public void onError(Throwable e) {
                SettableFuture<List<TransportProtos.TsKvProto>> callback = futures.remove(requestId);
                if (callback != null) {
                    callback.setException(e);
                }
            }
        });
        return future;
    }

    @Override
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse, TransportProtos.SessionInfoProto sessionInfo) {
        var callback = futures.remove(getAttributesResponse.getRequestId());
        if (callback != null) {
            callback.set(getAttributesResponse.getSharedAttributeListList());
        }
    }

    /**
     * Update - send request in change value resources in Client
     * 1. FirmwareUpdate:
     * - If msg.getSharedUpdatedList().forEach(tsKvProto -> {tsKvProto.getKv().getKey().indexOf(FIRMWARE_UPDATE_PREFIX, 0) == 0
     * 2. Shared Other AttributeUpdate
     * -- Path to resources from profile equal keyName or from ModelObject equal name
     * -- Only for resources:  isWritable && isPresent as attribute in profile -> LwM2MClientProfile (format: CamelCase)
     * 3. Delete - nothing
     *
     * @param msg -
     */
    @Override
    public void onAttributesUpdate(TransportProtos.AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2MClient = clientContext.getClientBySessionInfo(sessionInfo);
        if (msg.getSharedUpdatedCount() > 0 && lwM2MClient != null) {
            String newFirmwareTitle = null;
            String newFirmwareVersion = null;
            String newFirmwareTag = null;
            String newFirmwareUrl = null;
            String newSoftwareTitle = null;
            String newSoftwareVersion = null;
            String newSoftwareTag = null;
            String newSoftwareUrl = null;
            List<TransportProtos.TsKvProto> otherAttributes = new ArrayList<>();
            for (TransportProtos.TsKvProto tsKvProto : msg.getSharedUpdatedList()) {
                String attrName = tsKvProto.getKv().getKey();
                if (compareAttNameKeyOta(attrName)) {
                    if (FIRMWARE_TITLE.equals(attrName)) {
                        newFirmwareTitle = getStrValue(tsKvProto);
                    } else if (FIRMWARE_VERSION.equals(attrName)) {
                        newFirmwareVersion = getStrValue(tsKvProto);
                    } else if (FIRMWARE_TAG.equals(attrName)) {
                        newFirmwareTag = getStrValue(tsKvProto);
                    } else if (FIRMWARE_URL.equals(attrName)) {
                        newFirmwareUrl = getStrValue(tsKvProto);
                    } else if (SOFTWARE_TITLE.equals(attrName)) {
                        newSoftwareTitle = getStrValue(tsKvProto);
                    } else if (SOFTWARE_VERSION.equals(attrName)) {
                        newSoftwareVersion = getStrValue(tsKvProto);
                    } else if (SOFTWARE_TAG.equals(attrName)) {
                        newSoftwareTag = getStrValue(tsKvProto);
                    } else if (SOFTWARE_URL.equals(attrName)) {
                        newSoftwareUrl = getStrValue(tsKvProto);
                    }
                } else {
                    otherAttributes.add(tsKvProto);
                }
            }
            if (newFirmwareTitle != null || newFirmwareVersion != null) {
                otaUpdateService.onTargetFirmwareUpdate(lwM2MClient, newFirmwareTitle, newFirmwareVersion, Optional.ofNullable(newFirmwareUrl), Optional.ofNullable(newFirmwareTag));
            }
            if (newSoftwareTitle != null || newSoftwareVersion != null) {
                otaUpdateService.onTargetSoftwareUpdate(lwM2MClient, newSoftwareTitle, newSoftwareVersion, Optional.ofNullable(newSoftwareUrl), Optional.ofNullable(newSoftwareTag));
            }
            if (!otherAttributes.isEmpty()) {
                onAttributesUpdate(lwM2MClient, otherAttributes, true);
            }
        } else if (lwM2MClient == null) {
            log.error("OnAttributeUpdate, lwM2MClient is null");
        }
    }

    /**
     * #1.1 If two names have equal path => last time attribute
     * #2.1 if there is a difference in values between the current resource values and the shared attribute values
     * => send to client Request Update of value (new value from shared attribute)
     * and LwM2MClient.delayedRequests.add(path)
     * #2.1 if there is not a difference in values between the current resource values and the shared attribute values
     */
    @Override
    public void onAttributesUpdate(LwM2mClient lwM2MClient, List<TransportProtos.TsKvProto> tsKvProtos, boolean logFailedUpdateOfNonChangedValue) {
        log.trace("[{}] onAttributesUpdate [{}]", lwM2MClient.getEndpoint(), tsKvProtos);
        Map<String, TransportProtos.TsKvProto> attributesUpdate = new ConcurrentHashMap<>();
        tsKvProtos.forEach(tsKvProto -> {
            try {
                String pathIdVer = clientContext.getObjectIdByKeyNameFromProfile(lwM2MClient, tsKvProto.getKv().getKey());
                if (pathIdVer != null) {
                    // #1.1
                    if (lwM2MClient.getSharedAttributes().containsKey(pathIdVer)) {
                        if (tsKvProto.getTs() > lwM2MClient.getSharedAttributes().get(pathIdVer).getTs()) {
                            attributesUpdate.put(pathIdVer, tsKvProto);
                        }
                    } else {
                        attributesUpdate.put(pathIdVer, tsKvProto);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Failed update resource [" + lwM2MClient.getEndpoint() + "] onAttributesUpdate:", e);
                String logMsg = String.format("%s: Failed update resource onAttributesUpdate %s.",
                        LOG_LWM2M_ERROR, e.getMessage());
                logService.log(lwM2MClient, logMsg);
            }
        });
        clientContext.update(lwM2MClient);
        // #2.1
        attributesUpdate.forEach((pathIdVer, tsKvProto) -> {
            ResourceModel resourceModel = lwM2MClient.getResourceModel(pathIdVer, modelProvider);
            Object newValProto = getValueFromKvProto(tsKvProto.getKv());
            Object oldResourceValue = this.getResourceValueFormatKv(lwM2MClient, pathIdVer);
            if (!resourceModel.multiple || !(newValProto instanceof JsonElement)) {
                this.pushUpdateToClientIfNeeded(lwM2MClient, oldResourceValue, newValProto, pathIdVer, tsKvProto, logFailedUpdateOfNonChangedValue);
            } else {
                try {
                    pushUpdateMultiToClientIfNeeded(lwM2MClient, resourceModel, (JsonElement) newValProto,
                            (Map<Integer, LwM2mResourceInstance>) oldResourceValue, pathIdVer, tsKvProto, logFailedUpdateOfNonChangedValue);
                } catch (Exception e) {
                    log.error("Failed update resource [" + lwM2MClient.getEndpoint() + "] onAttributesUpdate:", e);
                    String logMsg = String.format("%s: Failed update resource onAttributesUpdate %s.",
                            LOG_LWM2M_ERROR, e.getMessage());
                    logService.log(lwM2MClient, logMsg);
                }
            }
        });
    }

    private void pushUpdateToClientIfNeeded(LwM2mClient lwM2MClient, Object oldValue, Object newValue,
                                            String versionedId, TransportProtos.TsKvProto tsKvProto, boolean logFailedUpdateOfNonChangedValue) {
        if (newValue == null) {
            String logMsg = String.format("%s: Failed update resource versionedId - %s value - %s. New value is  bad",
                    LOG_LWM2M_ERROR, versionedId, "null");
            logService.log(lwM2MClient, logMsg);
            log.error("Failed update resource [{}] [{}]", versionedId, "null");
        } else if ((oldValue == null) || !valueEquals(newValue, oldValue)) {
            TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId).value(newValue).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
            downlinkHandler.sendWriteReplaceRequest(lwM2MClient, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, lwM2MClient, versionedId) {
                @Override
                public void onSuccess(WriteRequest request, WriteResponse response) {
                    client.getSharedAttributes().put(versionedId, tsKvProto);
                    super.onSuccess(request, response);
                }
            });
        } else if (logFailedUpdateOfNonChangedValue) {
            String logMsg = String.format("%s: Didn't update the versionedId resource - %s value - %s. Value is not changed",
                    LOG_LWM2M_WARN, versionedId, newValue);
            logService.log(lwM2MClient, logMsg);
            log.warn("Didn't update resource [{}] [{}]. Value is not changed", versionedId, newValue);
        }
    }

    private void pushUpdateMultiToClientIfNeeded(LwM2mClient client, ResourceModel resourceModel, JsonElement newValProto,
                                                 Map<Integer, LwM2mResourceInstance> valueOld, String versionedId,
                                                 TransportProtos.TsKvProto tsKvProto, boolean logFailedUpdateOfNonChangedValue) {
        Map<Integer, Object> newValues = convertMultiResourceValuesFromJson(newValProto, resourceModel.type, versionedId);
        if (newValues.size() > 0 && valueOld != null && valueOld.size() > 0) {
            valueOld.values().forEach((v) -> {
                if (newValues.containsKey(v.getId())) {
                    if (valueEquals(newValues.get(v.getId()), v.getValue())) {
                        newValues.remove(v.getId());
                    }
                }
            });
        }

        if (newValues.size() > 0) {
            TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId).value(newValues).timeout(this.config.getTimeout()).build();
            downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId) {
                @Override
                public void onSuccess(WriteRequest request, WriteResponse response) {
                    client.getSharedAttributes().put(versionedId, tsKvProto);
                    super.onSuccess(request, response);
                }
            });
        } else if (logFailedUpdateOfNonChangedValue) {
            log.warn("Didn't update resource [{}] [{}]", versionedId, newValProto);
            String logMsg = String.format("%s: Didn't update resource versionedId - %s value - %s. Value is not changed",
                    LOG_LWM2M_WARN, versionedId, newValProto);
            logService.log(client, logMsg);
        }
    }

    /**
     * @param pathIdVer - path resource
     * @return - value of Resource into format KvProto or null
     */
    private Object getResourceValueFormatKv(LwM2mClient lwM2MClient, String pathIdVer) {
        LwM2mResource resourceValue = LwM2MTransportUtil.getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer);
        if (resourceValue != null) {
            ResourceModel.Type currentType = resourceValue.getType();
            ResourceModel.Type expectedType = helper.getResourceModelTypeEqualsKvProtoValueType(currentType, pathIdVer);
            if (!resourceValue.isMultiInstances()) {
                return LwM2mValueConverterImpl.getInstance().convertValue(resourceValue.getValue(), currentType, expectedType,
                        new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
            } else if (resourceValue.getInstances().size() > 0) {
                return resourceValue.getInstances();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getStrValue(TransportProtos.TsKvProto tsKvProto) {
        return tsKvProto.getKv().getStringV();
    }
}
