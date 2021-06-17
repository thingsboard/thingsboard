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
package org.thingsboard.server.transport.lwm2m.server.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.ContentFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2MFirmwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;
import org.thingsboard.server.transport.lwm2m.server.UpdateResultFw;
import org.thingsboard.server.transport.lwm2m.server.UpdateStateFw;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.ota.OtaPackageKey.STATE;
import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getAttributeKey;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FIRMWARE_UPDATE_COAP_RECOURSE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LWM2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertObjectIdToVersionedId;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MOtaUpdateService extends LwM2MExecutorAwareService implements LwM2MOtaUpdateService {

    public static final String FIRMWARE_VERSION = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION);
    public static final String FIRMWARE_TITLE = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE);
    public static final String FIRMWARE_URL = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.URL);
    public static final String SOFTWARE_VERSION = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION);
    public static final String SOFTWARE_TITLE = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE);
    public static final String SOFTWARE_URL = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.URL);

    private static final String FW_PACKAGE_5_ID = "/5/0/0";
    private static final String FW_URL_ID = "/5/0/1";
    private static final String FW_EXECUTE_ID = "/5/0/2";
    private static final String FW_NAME_ID = "/5/0/6";
    private static final String FW_VER_ID = "/5/0/7";
    private static final String SW_NAME_ID = "/9/0/0";
    private static final String SW_VER_ID = "/9/0/1";

    private final Map<String, LwM2MClientOtaInfo> fwStates = new ConcurrentHashMap<>();
    private final Map<String, LwM2MClientOtaInfo> swStates = new ConcurrentHashMap<>();

    private final TransportService transportService;
    private final LwM2mClientContext clientContext;
    private final LwM2MTransportServerConfig config;
    private final LwM2mUplinkMsgHandler uplinkHandler;
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    private final OtaPackageDataCache otaPackageDataCache;
    private final LwM2MTelemetryLogService logService;
    private final LwM2mTransportServerHelper helper;

    @Autowired
    @Lazy
    private LwM2MAttributesService attributesService;

    @PostConstruct
    public void init() {
        super.init();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    protected int getExecutorSize() {
        return config.getOtaPoolSize();
    }

    @Override
    protected String getExecutorName() {
        return "LwM2M OTA";
    }

    @Override
    public void init(LwM2mClient client) {
        //TODO: add locks by client fwInfo.
        //TODO: check that the client supports FW and SW by checking the supported objects in the model.
        List<String> attributesToFetch = new ArrayList<>();
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        if (fwInfo.isSupported()) {
            attributesToFetch.add(FIRMWARE_TITLE);
            attributesToFetch.add(FIRMWARE_VERSION);
            attributesToFetch.add(FIRMWARE_URL);
        }

        if (!attributesToFetch.isEmpty()) {
            var future = attributesService.getSharedAttributes(client, attributesToFetch);
            DonAsynchron.withCallback(future, attrs -> {
                if (fwInfo.isSupported()) {
                    Optional<String> newFirmwareTitle = getAttributeValue(attrs, FIRMWARE_TITLE);
                    Optional<String> newFirmwareVersion = getAttributeValue(attrs, FIRMWARE_VERSION);
                    Optional<String> newFirmwareUrl = getAttributeValue(attrs, FIRMWARE_URL);
                    if (newFirmwareTitle.isPresent() && newFirmwareVersion.isPresent()) {
                        onTargetFirmwareUpdate(client, newFirmwareTitle.get(), newFirmwareVersion.get(), newFirmwareUrl);
                    }
                }
            }, throwable -> {
                if (fwInfo.isSupported()) {
                    fwInfo.setTargetFetchFailure(true);
                }
            }, executor);
        }
    }

    @Override
    public void forceFirmwareUpdate(LwM2mClient client) {
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setRetryAttempts(0);
        fwInfo.setFailedPackageId(null);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public void onTargetFirmwareUpdate(LwM2mClient client, String newFirmwareTitle, String newFirmwareVersion, Optional<String> newFirmwareUrl) {
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.updateTarget(newFirmwareTitle, newFirmwareVersion, newFirmwareUrl);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public void onCurrentFirmwareNameUpdate(LwM2mClient client, String name) {
        log.debug("[{}] Current fw name: {}", client.getEndpoint(), name);
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentName(name);
    }

    @Override
    public void onCurrentFirmwareVersion3Update(LwM2mClient client, String version) {
        log.debug("[{}] Current fw version: {}", client.getEndpoint(), version);
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion3(version);
    }

    @Override
    public void onCurrentFirmwareVersion5Update(LwM2mClient client, String version) {
        log.debug("[{}] Current fw version: {}", client.getEndpoint(), version);
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion5(version);
    }

    @Override
    public void onCurrentFirmwareStateUpdate(LwM2mClient client, Long stateCode) {
        log.debug("[{}] Current fw state: {}", client.getEndpoint(), stateCode);
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        UpdateStateFw state = UpdateStateFw.fromStateFwByCode(stateCode.intValue());
        if (UpdateStateFw.DOWNLOADED.equals(state)) {
            executeFwUpdate(client);
        }
        fwInfo.setUpdateState(state);
        Optional<OtaPackageUpdateStatus> status = LwM2mTransportUtil.toOtaPackageUpdateStatus(state);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, fwInfo,
                otaStatus, "Firmware Update State: " + state.name()));
    }

    @Override
    public void onCurrentFirmwareResultUpdate(LwM2mClient client, Long code) {
        log.debug("[{}] Current fw result: {}", client.getEndpoint(), code);
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        UpdateResultFw result = UpdateResultFw.fromUpdateResultFwByCode(code.intValue());
        Optional<OtaPackageUpdateStatus> status = LwM2mTransportUtil.toOtaPackageUpdateStatus(result);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, fwInfo,
                otaStatus, "Firmware Update Result: " + result.name()));
        if (result.isAgain() && fwInfo.getRetryAttempts() <= 2) {
            fwInfo.setRetryAttempts(fwInfo.getRetryAttempts() + 1);
            startFirmwareUpdateIfNeeded(client, fwInfo);
        } else {
            fwInfo.setUpdateResult(result);
        }
    }

    @Override
    public void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient client, Long value) {
        log.debug("[{}] Current fw delivery method: {}", client.getEndpoint(), value);
        LwM2MClientOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setDeliveryMethod(value.intValue());
    }

    @Override
    public void onTargetSoftwareUpdate(LwM2mClient client, String newSoftwareTitle, String newSoftwareVersion) {

    }

    private void startFirmwareUpdateIfNeeded(LwM2mClient client, LwM2MClientOtaInfo fwInfo) {
        try {
            if (!fwInfo.isSupported()) {
                log.debug("[{}] Fw update is not supported: {}", client.getEndpoint(), fwInfo);
                sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Client does not support firmware update or profile misconfiguration!");
            } else if (fwInfo.isUpdateRequired()) {
                if (StringUtils.isNotEmpty(fwInfo.getTargetUrl())) {
                    log.debug("[{}] Starting update to [{}{}] using URL: {}", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion(), fwInfo.getTargetUrl());
                    startFirmwareUpdateUsingUrl(client, fwInfo.getTargetUrl());
                } else {
                    log.debug("[{}] Starting update to [{}{}] using binary", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion());
                    startFirmwareUpdateUsingBinary(client, fwInfo);
                }
            }
        } catch (Exception e) {
            log.warn("[{}] failed to update client: {}", client.getEndpoint(), fwInfo, e);
            sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    private void startFirmwareUpdateUsingUrl(LwM2mClient client, String url) {
        String targetIdVer = convertObjectIdToVersionedId(FW_URL_ID, client.getRegistration());
        TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(targetIdVer).value(url).timeout(config.getTimeout()).build();
        downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, targetIdVer));
    }

    public void startFirmwareUpdateUsingBinary(LwM2mClient client, LwM2MClientOtaInfo fwInfo) {
        String versionedId = convertObjectIdToVersionedId(FW_PACKAGE_5_ID, client.getRegistration());
        this.transportService.process(client.getSession(), createOtaPackageRequestMsg(client.getSession(), OtaPackageType.FIRMWARE.name()),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
                        executor.submit(() -> doUpdateFirmwareUsingBinary(response, fwInfo, versionedId, client));
                    }

                    @Override
                    public void onError(Throwable e) {
                        logService.log(client, "Failed to process firmware update: " + e.getMessage());
                    }
                });
    }

    private void doUpdateFirmwareUsingBinary(TransportProtos.GetOtaPackageResponseMsg response, LwM2MClientOtaInfo fwInfo, String versionedId, LwM2mClient client) {
        if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
            UUID otaPackageId = new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB());
            LwM2MFirmwareUpdateStrategy strategy;
            if (fwInfo.getDeliveryMethod() == null || fwInfo.getDeliveryMethod() == 2) {
                strategy = fwInfo.getStrategy();
            } else {
                strategy = fwInfo.getDeliveryMethod() == 0 ? LwM2MFirmwareUpdateStrategy.OBJ_5_TEMP_URL : LwM2MFirmwareUpdateStrategy.OBJ_5_BINARY;
            }
            switch (strategy) {
                case OBJ_5_BINARY:
                    byte[] firmwareChunk = otaPackageDataCache.get(otaPackageId.toString(), 0, 0);
                    TbLwM2MWriteReplaceRequest writeRequest = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId)
                            .value(firmwareChunk).contentFormat(ContentFormat.OPAQUE)
                            .timeout(config.getTimeout()).build();
                    downlinkHandler.sendWriteReplaceRequest(client, writeRequest, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId));
                    break;
                case OBJ_5_TEMP_URL:
                    startFirmwareUpdateUsingUrl(client, fwInfo.getBaseUrl() + "/" + FIRMWARE_UPDATE_COAP_RECOURSE + "/" + otaPackageId.toString());
                    break;
                default:
                    sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Unsupported strategy: " + strategy.name());
            }
        } else {
            sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Failed to fetch OTA package: " + response.getResponseStatus());
        }
    }

    private TransportProtos.GetOtaPackageRequestMsg createOtaPackageRequestMsg(TransportProtos.SessionInfoProto sessionInfo, String nameFwSW) {
        return TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                .setType(nameFwSW)
                .build();
    }

    private void executeFwUpdate(LwM2mClient client) {
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(FW_EXECUTE_ID).timeout(config.getTimeout()).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, FW_EXECUTE_ID));
    }

    private Optional<String> getAttributeValue(List<TransportProtos.TsKvProto> attrs, String keyName) {
        for (TransportProtos.TsKvProto attr : attrs) {
            if (keyName.equals(attr.getKv().getKey())) {
                if (attr.getKv().getType().equals(TransportProtos.KeyValueType.STRING_V)) {
                    return Optional.of(attr.getKv().getStringV());
                } else {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private LwM2MClientOtaInfo getOrInitFwInfo(LwM2mClient client) {
        //TODO: fetch state from the cache or DB.
        return fwStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            var profile = clientContext.getProfile(client.getProfileId());
            return new LwM2MClientOtaInfo(endpoint, OtaPackageType.FIRMWARE, profile.getClientLwM2mSettings().getFwUpdateStrategy(),
                    profile.getClientLwM2mSettings().getFwUpdateRecourse());
        });
    }

    private LwM2MClientOtaInfo getOrInitSwInfo(LwM2mClient client) {
        //TODO: fetch state from the cache or DB.
        return swStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            var profile = clientContext.getProfile(client.getProfileId());
            return new LwM2MClientOtaInfo(endpoint, OtaPackageType.SOFTWARE, profile.getClientLwM2mSettings().getSwUpdateStrategy(), profile.getClientLwM2mSettings().getSwUpdateRecourse());
        });

    }

    private void sendStateUpdateToTelemetry(LwM2mClient client, LwM2MClientOtaInfo fwInfo, OtaPackageUpdateStatus status, String log) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        TransportProtos.KeyValueProto.Builder kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(getAttributeKey(fwInfo.getType(), STATE));
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(status.name());
        result.add(kvProto.build());
        kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(LOG_LWM2M_TELEMETRY);
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(log);
        result.add(kvProto.build());
        helper.sendParametersOnThingsboardTelemetry(result, client.getSession());
    }

}
