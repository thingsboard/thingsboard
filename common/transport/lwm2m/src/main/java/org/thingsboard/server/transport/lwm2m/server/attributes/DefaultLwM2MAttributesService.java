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
package org.thingsboard.server.transport.lwm2m.server.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper.getValueFromKvProto;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.fromVersionedIdToObjectId;

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
            String newFirmwareUrl = null;
            String newSoftwareTitle = null;
            String newSoftwareVersion = null;
            List<TransportProtos.TsKvProto> otherAttributes = new ArrayList<>();
            for (TransportProtos.TsKvProto tsKvProto : msg.getSharedUpdatedList()) {
                String attrName = tsKvProto.getKv().getKey();
                if (DefaultLwM2MOtaUpdateService.FIRMWARE_TITLE.equals(attrName)) {
                    newFirmwareTitle = getStrValue(tsKvProto);
                } else if (DefaultLwM2MOtaUpdateService.FIRMWARE_VERSION.equals(attrName)) {
                    newFirmwareVersion = getStrValue(tsKvProto);
                } else if (DefaultLwM2MOtaUpdateService.FIRMWARE_URL.equals(attrName)) {
                    newFirmwareUrl = getStrValue(tsKvProto);
                } else if (DefaultLwM2MOtaUpdateService.SOFTWARE_TITLE.equals(attrName)) {
                    newSoftwareTitle = getStrValue(tsKvProto);
                } else if (DefaultLwM2MOtaUpdateService.SOFTWARE_VERSION.equals(attrName)) {
                    newSoftwareVersion = getStrValue(tsKvProto);
                } else {
                    otherAttributes.add(tsKvProto);
                }
            }
            if (newFirmwareTitle != null || newFirmwareVersion != null) {
                otaUpdateService.onTargetFirmwareUpdate(lwM2MClient, newFirmwareTitle, newFirmwareVersion, Optional.ofNullable(newFirmwareUrl));
            }
            if (newSoftwareTitle != null || newSoftwareVersion != null) {
                otaUpdateService.onTargetSoftwareUpdate(lwM2MClient, newSoftwareTitle, newSoftwareVersion);
            }
            if (!otherAttributes.isEmpty()) {
                onAttributesUpdate(lwM2MClient, otherAttributes);
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
     *
     */
    @Override
    public void onAttributesUpdate(LwM2mClient lwM2MClient, List<TransportProtos.TsKvProto> tsKvProtos) {
        log.trace("[{}] onAttributesUpdate [{}]", lwM2MClient.getEndpoint(), tsKvProtos);
        tsKvProtos.forEach(tsKvProto -> {
            String pathIdVer = clientContext.getObjectIdByKeyNameFromProfile(lwM2MClient, tsKvProto.getKv().getKey());
            if (pathIdVer != null) {
                // #1.1
                if (lwM2MClient.getSharedAttributes().containsKey(pathIdVer)) {
                    if (tsKvProto.getTs() > lwM2MClient.getSharedAttributes().get(pathIdVer).getTs()) {
                        lwM2MClient.getSharedAttributes().put(pathIdVer, tsKvProto);
                    }
                } else {
                    lwM2MClient.getSharedAttributes().put(pathIdVer, tsKvProto);
                }
            }
        });
        // #2.1
        lwM2MClient.getSharedAttributes().forEach((pathIdVer, tsKvProto) -> {
            this.pushUpdateToClientIfNeeded(lwM2MClient, this.getResourceValueFormatKv(lwM2MClient, pathIdVer),
                    getValueFromKvProto(tsKvProto.getKv()), pathIdVer);
        });
    }

    private void pushUpdateToClientIfNeeded(LwM2mClient lwM2MClient, Object valueOld, Object newValue, String versionedId) {
        if (newValue != null && (valueOld == null || !newValue.toString().equals(valueOld.toString()))) {
            TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId).value(newValue).timeout(this.config.getTimeout()).build();
            downlinkHandler.sendWriteReplaceRequest(lwM2MClient, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, lwM2MClient, versionedId));
        } else {
            log.error("Failed update resource [{}] [{}]", versionedId, newValue);
            String logMsg = String.format("%s: Failed update resource versionedId - %s value - %s. Value is not changed or bad",
                    LOG_LWM2M_ERROR, versionedId, newValue);
            logService.log(lwM2MClient, logMsg);
            log.info("Failed update resource [{}] [{}]", versionedId, newValue);
        }
    }

    /**
     * @param pathIdVer - path resource
     * @return - value of Resource into format KvProto or null
     */
    private Object getResourceValueFormatKv(LwM2mClient lwM2MClient, String pathIdVer) {
        LwM2mResource resourceValue = LwM2mTransportUtil.getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer);
        if (resourceValue != null) {
            ResourceModel.Type currentType = resourceValue.getType();
            ResourceModel.Type expectedType = helper.getResourceModelTypeEqualsKvProtoValueType(currentType, pathIdVer);
            return LwM2mValueConverterImpl.getInstance().convertValue(resourceValue.getValue(), currentType, expectedType,
                    new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
        } else {
            return null;
        }
    }

    private String getStrValue(TransportProtos.TsKvProto tsKvProto) {
        return tsKvProto.getKv().getStringV();
    }
}
