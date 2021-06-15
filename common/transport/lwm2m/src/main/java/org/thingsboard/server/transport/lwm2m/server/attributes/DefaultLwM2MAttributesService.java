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
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper.getValueFromKvProto;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.isFwSwWords;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MAttributesService implements LwM2MAttributesService {

    //TODO: add timeout logic
    private final AtomicInteger reqIdSeq = new AtomicInteger();
    private final Map<Integer, SettableFuture<List<TransportProtos.TsKvProto>>> futures;

    private final TransportService transportService;

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
    public void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo) {
//        LwM2mClient lwM2MClient = clientContext.getClientBySessionInfo(sessionInfo);
//        if (msg.getSharedUpdatedCount() > 0 && lwM2MClient != null) {
//            log.warn("2) OnAttributeUpdate, SharedUpdatedList() [{}]", msg.getSharedUpdatedList());
//            msg.getSharedUpdatedList().forEach(tsKvProto -> {
//                String pathName = tsKvProto.getKv().getKey();
//                String pathIdVer = this.getObjectIdByKeyNameFromProfile(sessionInfo, pathName);
//                Object valueNew = getValueFromKvProto(tsKvProto.getKv());
//                if ((OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION).equals(pathName)
//                        && (!valueNew.equals(lwM2MClient.getFwUpdate().getCurrentVersion())))
//                        || (OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE).equals(pathName)
//                        && (!valueNew.equals(lwM2MClient.getFwUpdate().getCurrentTitle())))) {
//                    this.getInfoFirmwareUpdate(lwM2MClient, null);
//                } else if ((OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION).equals(pathName)
//                        && (!valueNew.equals(lwM2MClient.getSwUpdate().getCurrentVersion())))
//                        || (OtaPackageUtil.getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE).equals(pathName)
//                        && (!valueNew.equals(lwM2MClient.getSwUpdate().getCurrentTitle())))) {
//                    this.getInfoSoftwareUpdate(lwM2MClient, null);
//                }
//                if (pathIdVer != null) {
//                    ResourceModel resourceModel = lwM2MClient.getResourceModel(pathIdVer, this.config
//                            .getModelProvider());
//                    if (resourceModel != null && resourceModel.operations.isWritable()) {
//                        this.updateResourcesValueToClient(lwM2MClient, this.getResourceValueFormatKv(lwM2MClient, pathIdVer), valueNew, pathIdVer);
//                    } else {
//                        log.error("Resource path - [{}] value - [{}] is not Writable and cannot be updated", pathIdVer, valueNew);
//                        String logMsg = String.format("%s: attributeUpdate: Resource path - %s value - %s is not Writable and cannot be updated",
//                                LOG_LWM2M_ERROR, pathIdVer, valueNew);
//                        this.logToTelemetry(lwM2MClient, logMsg);
//                    }
//                } else if (!isFwSwWords(pathName)) {
//                    log.error("Resource name name - [{}] value - [{}] is not present as attribute/telemetry in profile and cannot be updated", pathName, valueNew);
//                    String logMsg = String.format("%s: attributeUpdate: attribute name - %s value - %s is not present as attribute in profile and cannot be updated",
//                            LOG_LWM2M_ERROR, pathName, valueNew);
//                    this.logToTelemetry(lwM2MClient, logMsg);
//                }
//
//            });
//        } else if (msg.getSharedDeletedCount() > 0 && lwM2MClient != null) {
//            msg.getSharedUpdatedList().forEach(tsKvProto -> {
//                String pathName = tsKvProto.getKv().getKey();
//                Object valueNew = getValueFromKvProto(tsKvProto.getKv());
//                if (OtaPackageUtil.getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION).equals(pathName) && !valueNew.equals(lwM2MClient.getFwUpdate().getCurrentVersion())) {
//                    lwM2MClient.getFwUpdate().setCurrentVersion((String) valueNew);
//                }
//            });
//            log.info("[{}] delete [{}]  onAttributeUpdate", msg.getSharedDeletedList(), sessionInfo);
//        } else if (lwM2MClient == null) {
//            log.error("OnAttributeUpdate, lwM2MClient is null");
//        }
    }
}
