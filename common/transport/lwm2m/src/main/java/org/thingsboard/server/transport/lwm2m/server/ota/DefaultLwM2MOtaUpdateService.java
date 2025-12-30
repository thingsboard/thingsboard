/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.ota.OtaPackageKey;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCreateRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCreateResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareDeliveryMethod;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MFirmwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MSoftwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.rpc.RpcCreateRequest;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MClientOtaInfoStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.ota.OtaPackageKey.STATE;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;
import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getAttributeKey;
import static org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult.UPDATE_SUCCESSFULLY;
import static org.thingsboard.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult.NOT_ENOUGH_STORAGE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MOtaUpdateService extends LwM2MExecutorAwareService implements LwM2MOtaUpdateService {

    public static final String FIRMWARE_VERSION = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.VERSION);
    public static final String FIRMWARE_TITLE = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TITLE);
    public static final String FIRMWARE_TAG = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.TAG);
    public static final String FIRMWARE_URL = getAttributeKey(OtaPackageType.FIRMWARE, OtaPackageKey.URL);
    public static final String SOFTWARE_VERSION = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.VERSION);
    public static final String SOFTWARE_TITLE = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TITLE);
    public static final String SOFTWARE_TAG = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.TAG);
    public static final String SOFTWARE_URL = getAttributeKey(OtaPackageType.SOFTWARE, OtaPackageKey.URL);

    public static final String FIRMWARE_UPDATE_COAP_RESOURCE = "tbfw";
    public static final String SOFTWARE_UPDATE_COAP_RESOURCE = "tbsw";
    private static final String FW_PACKAGE_5_ID = "/5/0/0";
    private static final String FW_PACKAGE_19_ID = "/19/0/0";
    private static final String FW_URL_ID = "/5/0/1";
    private static final String FW_EXECUTE_ID = "/5/0/2";
    public static final String FW_STATE_ID = "/5/0/3";
    public static final String FW_RESULT_ID = "/5/0/5";
    public static final String FW_NAME_ID = "/5/0/6";
    public static final String FW_VER_ID = "/5/0/7";

    public static final String FW_3_VER_ID = "/3/0/3";
    public static final String FW_DELIVERY_METHOD = "/5/0/9";

    public static final String SW_3_VER_ID = "/3/0/19";

    public static final String SW_NAME_ID = "/9/0/0";
    public static final String SW_VER_ID = "/9/0/1";
    public static final String SW_PACKAGE_ID = "/9/0/2";
    public static final String SW_PACKAGE_URI_ID = "/9/0/3";
    public static final String SW_INSTALL_ID = "/9/0/4";
    public static final String SW_STATE_ID = "/9/0/7";
    public static final String SW_RESULT_ID = "/9/0/9";
    public static final String SW_UN_INSTALL_ID = "/9/0/6";

    public static final int FW_INSTANCE_ID = 65533;
    public static final String FW_INFO_19_INSTANCE_ID = "/19/" + FW_INSTANCE_ID;
    public static final int SW_INSTANCE_ID = 65534;
    public static final String SW_INFO_19_INSTANCE_ID = "/19/" + SW_INSTANCE_ID;
    public static final String OTA_INFO_19_TITLE = "title";
    public static final String OTA_INFO_19_VERSION = "version";
    public static final String OTA_INFO_19_FILE_CHECKSUM256 = "checksum";
    public static final String OTA_INFO_19_FILE_SIZE = "dataSize";
    public static final String OTA_INFO_19_FILE_NAME = "fileName";

    private final Map<String, LwM2MClientFwOtaInfo> fwStates = new ConcurrentHashMap<>();
    private final Map<String, LwM2MClientSwOtaInfo> swStates = new ConcurrentHashMap<>();

    private final TransportService transportService;
    private final LwM2mClientContext clientContext;
    private final LwM2MTransportServerConfig config;
    private final LwM2mUplinkMsgHandler uplinkHandler;
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    private final OtaPackageDataCache otaPackageDataCache;
    private final LwM2MTelemetryLogService logService;
    private final LwM2mTransportServerHelper helper;
    private final TbLwM2MClientOtaInfoStore otaInfoStore;
    private final LwM2mVersionedModelProvider modelProvider;

    @Autowired
    @Lazy
    private LwM2MAttributesService attributesService;

    @PostConstruct
    public void init() {
        super.init();
    }

    @PreDestroy
    public void destroy() {
        log.trace("Destroying {}", getClass().getSimpleName());
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
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);

        if (fwInfo.isSupported()) {
            attributesToFetch.add(FIRMWARE_TITLE);
            attributesToFetch.add(FIRMWARE_VERSION);
            attributesToFetch.add(FIRMWARE_TAG);
            attributesToFetch.add(FIRMWARE_URL);
        }

        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        if (swInfo.isSupported()) {
            attributesToFetch.add(SOFTWARE_TITLE);
            attributesToFetch.add(SOFTWARE_VERSION);
            attributesToFetch.add(SOFTWARE_TAG);
            attributesToFetch.add(SOFTWARE_URL);
        }

        var clientProfile = clientContext.getProfile(client.getRegistration());
        var clientSettings = clientProfile != null ? clientProfile.getClientLwM2mSettings() : null;
        if (clientSettings != null) {
            initFwStrategy(client, clientSettings);
            initSwStrategy(client, clientSettings);


            if (!attributesToFetch.isEmpty()) {
                var future = attributesService.getSharedAttributes(client, attributesToFetch);
                DonAsynchron.withCallback(future, attrs -> {
                    if (fwInfo.isSupported()) {
                        Optional<String> newFwTitle = getAttributeValue(attrs, FIRMWARE_TITLE);
                        Optional<String> newFwVersion = getAttributeValue(attrs, FIRMWARE_VERSION);
                        Optional<String> newFwTag = getAttributeValue(attrs, FIRMWARE_TAG);
                        Optional<String> newFwUrl = getAttributeValue(attrs, FIRMWARE_URL);
                        if (newFwTitle.isPresent() && newFwVersion.isPresent() && !isOtaDownloading(client) && !UPDATING.equals(fwInfo.status)) {
                            onTargetFirmwareUpdate(client, newFwTitle.get(), newFwVersion.get(), newFwUrl, newFwTag);
                        }
                    }
                    if (swInfo.isSupported()) {
                        Optional<String> newSwTitle = getAttributeValue(attrs, SOFTWARE_TITLE);
                        Optional<String> newSwVersion = getAttributeValue(attrs, SOFTWARE_VERSION);
                        Optional<String> newSwTag = getAttributeValue(attrs, SOFTWARE_TAG);
                        Optional<String> newSwUrl = getAttributeValue(attrs, SOFTWARE_URL);
                        if (newSwTitle.isPresent() && newSwVersion.isPresent()) {
                            onTargetSoftwareUpdate(client, newSwTitle.get(), newSwVersion.get(), newSwUrl, newSwTag);
                        }
                    }

                }, throwable -> {
                    if (fwInfo.isSupported()) {
                        update(fwInfo);
                    }
                }, executor);
            }
        }
    }

    @Override
    public void forceFirmwareUpdate(LwM2mClient client) {
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setRetryAttempts(0);
        fwInfo.setFailedPackageId(null);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public void onTargetFirmwareUpdate(LwM2mClient client, String newFirmwareTitle, String newFirmwareVersion, Optional<String> newFirmwareUrl, Optional<String> newFirmwareTag) {
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.updateTarget(newFirmwareTitle, newFirmwareVersion, newFirmwareUrl, newFirmwareTag);
        update(fwInfo);
        startFirmwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public void onCurrentFirmwareNameUpdate(LwM2mClient client, String name) {
        log.trace("[{}] Current fw name: {}", client.getEndpoint(), name);
        getOrInitFwInfo(client).setCurrentName(name);
    }

    @Override
    public void onCurrentSoftwareNameUpdate(LwM2mClient client, String name) {
        log.trace("[{}] Current sw name: {}", client.getEndpoint(), name);
        getOrInitSwInfo(client).setCurrentName(name);
    }

    @Override
    public void onFirmwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration) {
        log.trace("[{}] Current fw strategy: {}", client.getEndpoint(), configuration.getFwUpdateStrategy());
        startFirmwareUpdateIfNeeded(client, initFwStrategy(client, configuration));
    }

    private LwM2MClientFwOtaInfo initFwStrategy(LwM2mClient client, OtherConfiguration configuration) {
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setStrategy(LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(configuration.getFwUpdateStrategy()));
        fwInfo.setBaseUrl(configuration.getFwUpdateResource());
        return fwInfo;
    }

    @Override
    public void onCurrentSoftwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration) {
        log.trace("[{}] Current sw strategy: {}", client.getEndpoint(), configuration.getSwUpdateStrategy());
        startSoftwareUpdateIfNeeded(client, initSwStrategy(client, configuration));
    }

    private LwM2MClientSwOtaInfo initSwStrategy(LwM2mClient client, OtherConfiguration configuration) {
        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        swInfo.setStrategy(LwM2MSoftwareUpdateStrategy.fromStrategySwByCode(configuration.getSwUpdateStrategy()));
        swInfo.setBaseUrl(configuration.getSwUpdateResource());
        return swInfo;
    }

    @Override
    public void onCurrentFirmwareVersion3Update(LwM2mClient client, String version) {
        log.trace("[{}] Current fw version(3): {}", client.getEndpoint(), version);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion3(version);
    }

    @Override
    public void onCurrentFirmwareVersionUpdate(LwM2mClient client, String version) {
        log.trace("[{}] Current fw version(5): {}", client.getEndpoint(), version);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setCurrentVersion(version);
    }

    @Override
    public void onCurrentFirmwareStateUpdate(LwM2mClient client, Long stateCode) {
        log.trace("[{}] Current fw state: {}", client.getEndpoint(), stateCode);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        FirmwareUpdateState state = FirmwareUpdateState.fromStateFwByCode(stateCode.intValue());
        if (FirmwareUpdateState.DOWNLOADED.equals(state)) {
            executeFwUpdate(client);
        }
        fwInfo.setUpdateState(state);
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(state);

        if (FirmwareUpdateState.IDLE.equals(state) && DOWNLOADING.equals(fwInfo.getStatus())) {
            fwInfo.setFailedPackageId(fwInfo.getTargetPackageId());
            status = Optional.of(FAILED);
        }

        status.ifPresent(otaStatus -> {
            fwInfo.setStatus(otaStatus);
            sendStateUpdateToTelemetry(client, fwInfo,
                    otaStatus, "Firmware Update State: " + state.name());
        });
        update(fwInfo);
    }

    @Override
    public void onCurrentFirmwareResultUpdate(LwM2mClient client, Long code) {
        log.trace("[{}] Current fw result: {}", client.getEndpoint(), code);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        FirmwareUpdateResult result = FirmwareUpdateResult.fromUpdateResultFwByCode(code.intValue());
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(result);

        if (FirmwareUpdateResult.INITIAL.equals(result) && OtaPackageUpdateStatus.UPDATING.equals(fwInfo.getStatus())) {
            status = Optional.of(UPDATED);
            fwInfo.setRetryAttempts(0);
            fwInfo.setFailedPackageId(null);
        }

        status.ifPresent(otaStatus -> {
                    fwInfo.setStatus(otaStatus);
                    sendStateUpdateToTelemetry(client, fwInfo,
                            otaStatus, "Firmware Update Result: " + result.name());
                }
        );

        if (result.isAgain() && fwInfo.getRetryAttempts() <= 2) {
            fwInfo.setRetryAttempts(fwInfo.getRetryAttempts() + 1);
            startFirmwareUpdateIfNeeded(client, fwInfo);
        } else {
            fwInfo.update(result);
        }
        update(fwInfo);
    }

    @Override
    public void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient client, Long value) {
        log.trace("[{}] Current fw delivery method: {}", client.getEndpoint(), value);
        LwM2MClientFwOtaInfo fwInfo = getOrInitFwInfo(client);
        fwInfo.setDeliveryMethod(value.intValue());
    }

    @Override
    public void onCurrentSoftwareVersion3Update(LwM2mClient client, String version) {
        log.trace("[{}] Current sw version(3): {}", client.getEndpoint(), version);
        getOrInitSwInfo(client).setCurrentVersion3(version);
    }

    @Override
    public void onCurrentSoftwareVersionUpdate(LwM2mClient client, String version) {
        log.trace("[{}] Current sw version(9): {}", client.getEndpoint(), version);
        getOrInitSwInfo(client).setCurrentVersion(version);
    }

    @Override
    public void onCurrentSoftwareStateUpdate(LwM2mClient client, Long stateCode) {
        log.trace("[{}] Current sw state: {}", client.getEndpoint(), stateCode);
        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        SoftwareUpdateState state = SoftwareUpdateState.fromUpdateStateSwByCode(stateCode.intValue());
        if (SoftwareUpdateState.INITIAL.equals(state)) {
            startSoftwareUpdateIfNeeded(client, swInfo);
        } else if (SoftwareUpdateState.DELIVERED.equals(state)) {
            executeSwInstall(client);
        }
        swInfo.setUpdateState(state);
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(state);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, swInfo,
                otaStatus, "Firmware Update State: " + state.name()));
        update(swInfo);
    }


    @Override
    public void onCurrentSoftwareResultUpdate(LwM2mClient client, Long code) {
        log.trace("[{}] Current sw result: {}", client.getEndpoint(), code);
        LwM2MClientSwOtaInfo swInfo = getOrInitSwInfo(client);
        SoftwareUpdateResult result = SoftwareUpdateResult.fromUpdateResultSwByCode(code.intValue());
        Optional<OtaPackageUpdateStatus> status = toOtaPackageUpdateStatus(result);
        status.ifPresent(otaStatus -> sendStateUpdateToTelemetry(client, swInfo,
                otaStatus, "Software Update Result: " + result.name()));
        if (result.isAgain() && swInfo.getRetryAttempts() <= 2) {
            swInfo.setRetryAttempts(swInfo.getRetryAttempts() + 1);
            startSoftwareUpdateIfNeeded(client, swInfo);
        } else {
            swInfo.update(result);
        }
        update(swInfo);
    }

    @Override
    public void onTargetSoftwareUpdate(LwM2mClient client, String newSoftwareTitle, String newSoftwareVersion, Optional<String> newSoftwareUrl, Optional<String> newSoftwareTag) {
        LwM2MClientSwOtaInfo fwInfo = getOrInitSwInfo(client);
        fwInfo.updateTarget(newSoftwareTitle, newSoftwareVersion, newSoftwareUrl, newSoftwareTag);
        update(fwInfo);
        startSoftwareUpdateIfNeeded(client, fwInfo);
    }

    @Override
    public boolean isOtaDownloading(LwM2mClient client) {
        String endpoint = client.getEndpoint();
        LwM2MClientFwOtaInfo fwInfo = fwStates.get(endpoint);
        LwM2MClientSwOtaInfo swInfo = swStates.get(endpoint);

        if (fwInfo != null && (DOWNLOADING.equals(fwInfo.getStatus()))) {
            return true;
        }
        if (swInfo != null && (DOWNLOADING.equals(swInfo.getStatus()))) {
            return true;
        }

        return false;
    }

    private void startFirmwareUpdateIfNeeded(LwM2mClient client, LwM2MClientFwOtaInfo fwInfo) {
        try {
            if (!fwInfo.isSupported() && fwInfo.isAssigned()) {
                log.trace("[{}] Fw update is not supported: {}", client.getEndpoint(), fwInfo);
                sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Client does not support firmware update or profile misconfiguration!");
            } else if (fwInfo.isUpdateRequired()) {
                if (StringUtils.isNotEmpty(fwInfo.getTargetUrl())) {
                    log.trace("[{}] Starting update to [{}{}][] using URL: {}", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion(), fwInfo.getTargetUrl());
                    startUpdateUsingUrl(client, FW_URL_ID, fwInfo.getTargetUrl());
                } else {
                    log.trace("[{}] Starting update to [{}{}] using binary", client.getEndpoint(), fwInfo.getTargetName(), fwInfo.getTargetVersion());
                    startUpdateUsingBinary(client, fwInfo);
                }
            } else if (fwInfo.getResult() != null && fwInfo.getResult().getCode() > UPDATE_SUCCESSFULLY.getCode()) {
                log.trace("[{}] Previous update failed. [{}]", client.getEndpoint(), fwInfo);
                logService.log(client, "Previous update firmware failed. Result: " + fwInfo.getResult().name());
            }
        } catch (Exception e) {
            log.error("[{}] failed to update client: {}", client.getEndpoint(), fwInfo, e);
            sendStateUpdateToTelemetry(client, fwInfo, OtaPackageUpdateStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    private void startSoftwareUpdateIfNeeded(LwM2mClient client, LwM2MClientSwOtaInfo swInfo) {
        try {
            if (!swInfo.isSupported() && swInfo.isAssigned()) {
                log.trace("[{}] Sw update is not supported: {}", client.getEndpoint(), swInfo);
                sendStateUpdateToTelemetry(client, swInfo, OtaPackageUpdateStatus.FAILED, "Client does not support software update or profile misconfiguration!");
            } else if (swInfo.isUpdateRequired()) {
                if (SoftwareUpdateState.INSTALLED.equals(swInfo.getUpdateState())) {
                    log.trace("[{}] Attempt to restore the update state: {}", client.getEndpoint(), swInfo.getUpdateState());
                    executeSwUninstallForUpdate(client);
                } else {
                    if (StringUtils.isNotEmpty(swInfo.getTargetUrl())) {
                        log.trace("[{}] Starting update to [{}{}] using URL: {}", client.getEndpoint(), swInfo.getTargetName(), swInfo.getTargetVersion(), swInfo.getTargetUrl());
                        startUpdateUsingUrl(client, SW_PACKAGE_URI_ID, swInfo.getTargetUrl());
                    } else {
                        log.trace("[{}] Starting update to [{}{}] using binary", client.getEndpoint(), swInfo.getTargetName(), swInfo.getTargetVersion());
                        startUpdateUsingBinary(client, swInfo);
                    }
                }
            } else if (swInfo.getResult() != null && swInfo.getResult().getCode() >= NOT_ENOUGH_STORAGE.getCode()) {
                log.trace("[{}] Previous update failed. [{}]", client.getEndpoint(), swInfo);
                logService.log(client, "Previous update software failed. Result: " + swInfo.getResult().name());
            }
        } catch (Exception e) {
            log.info("[{}] failed to update client: {}", client.getEndpoint(), swInfo, e);
            sendStateUpdateToTelemetry(client, swInfo, OtaPackageUpdateStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    public void startUpdateUsingBinary(LwM2mClient client, LwM2MClientSwOtaInfo swInfo) {
        this.transportService.process(client.getSession(), createOtaPackageRequestMsg(client.getSession(), swInfo.getType().name()),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
                        executor.submit(() -> doUpdateSoftwareUsingBinary(response, swInfo, client));
                    }

                    @Override
                    public void onError(Throwable e) {
                        logService.log(client, "Failed to process software update: " + e.getMessage());
                    }
                });
    }

    private void startUpdateUsingUrl(LwM2mClient client, String id, String url) {
        String targetIdVer = convertObjectIdToVersionedId(id, client);
        TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(targetIdVer).value(url).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, targetIdVer));
    }

    public void startUpdateUsingBinary(LwM2mClient client, LwM2MClientFwOtaInfo fwInfo) {
        this.transportService.process(client.getSession(), createOtaPackageRequestMsg(client.getSession(), fwInfo.getType().name()),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
                        executor.submit(() -> doUpdateFirmwareUsingBinary(response, fwInfo, client));
                    }

                    @Override
                    public void onError(Throwable e) {
                        logService.log(client, "Failed to process firmware update: " + e.getMessage());
                    }
                });
    }

    private void doUpdateFirmwareUsingBinary(TransportProtos.GetOtaPackageResponseMsg response, LwM2MClientFwOtaInfo info, LwM2mClient client) {
        if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
            UUID otaPackageId = new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB());
            LwM2MFirmwareUpdateStrategy strategy;
            if (info.getDeliveryMethod() == null || info.getDeliveryMethod() == FirmwareDeliveryMethod.BOTH.code) {
                strategy = info.getStrategy();
            } else {
                strategy = info.getDeliveryMethod() == FirmwareDeliveryMethod.PULL.code ? LwM2MFirmwareUpdateStrategy.OBJ_5_TEMP_URL : LwM2MFirmwareUpdateStrategy.OBJ_5_BINARY;
            }
            var clientProfile =  clientContext.getProfile(client.getRegistration());
            Boolean useObject19ForOtaInfo = clientProfile != null ? clientProfile.getClientLwM2mSettings().getUseObject19ForOtaInfo() : null;
            if (useObject19ForOtaInfo != null && useObject19ForOtaInfo){
                sendInfoToObject19ForOta(client, FW_INFO_19_INSTANCE_ID, response, otaPackageId);
            }
            switch (strategy) {
                case OBJ_5_BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(FW_PACKAGE_5_ID, client), otaPackageId);
                    break;
                case OBJ_19_BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(FW_PACKAGE_19_ID, client), otaPackageId);
                    break;
                case OBJ_5_TEMP_URL:
                    startUpdateUsingUrl(client, FW_URL_ID, info.getBaseUrl() + "/" + FIRMWARE_UPDATE_COAP_RESOURCE + "/" + otaPackageId.toString());
                    break;
                default:
                    sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Unsupported strategy: " + strategy.name());
            }
        } else {
            sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Failed to fetch OTA package: " + response.getResponseStatus());
        }
    }

    private void doUpdateSoftwareUsingBinary(TransportProtos.GetOtaPackageResponseMsg response, LwM2MClientSwOtaInfo info, LwM2mClient client) {
        if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
            UUID otaPackageId = new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB());
            LwM2MSoftwareUpdateStrategy strategy = info.getStrategy();
            var clientProfile = clientContext.getProfile(client.getRegistration());
            Boolean useObject19ForOtaInfo = clientProfile != null ? clientProfile.getClientLwM2mSettings().getUseObject19ForOtaInfo() : null;
            if (useObject19ForOtaInfo != null && useObject19ForOtaInfo){
                sendInfoToObject19ForOta(client, SW_INFO_19_INSTANCE_ID, response, otaPackageId);
            }
            switch (strategy) {
                case BINARY:
                    startUpdateUsingBinary(client, convertObjectIdToVersionedId(SW_PACKAGE_ID, client), otaPackageId);
                    break;
                case TEMP_URL:
                    startUpdateUsingUrl(client, SW_PACKAGE_URI_ID, info.getBaseUrl() + "/" + FIRMWARE_UPDATE_COAP_RESOURCE + "/" + otaPackageId.toString());
                    break;
                default:
                    sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Unsupported strategy: " + strategy.name());
            }
        } else {
            sendStateUpdateToTelemetry(client, info, OtaPackageUpdateStatus.FAILED, "Failed to fetch OTA package: " + response.getResponseStatus());
        }
    }

    private void startUpdateUsingBinary(LwM2mClient client, String versionedId, UUID otaPackageId) {
        byte[] firmwareChunk = otaPackageDataCache.get(otaPackageId.toString(), 0, 0);
        TbLwM2MWriteReplaceRequest writeRequest = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId)
                .value(firmwareChunk).contentFormat(ContentFormat.OPAQUE)
                .timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendWriteReplaceRequest(client, writeRequest, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId));
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
        log.trace("[{}] Execute SW [{}]", client.getEndpoint(), FW_EXECUTE_ID);
        String fwExecuteVerId = convertObjectIdToVersionedId(FW_EXECUTE_ID, client);
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(fwExecuteVerId).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, fwExecuteVerId));
    }

    private void executeSwInstall(LwM2mClient client) {
        log.trace("[{}] Execute SW (install) [{}]", client.getEndpoint(), SW_INSTALL_ID);
        String swInstallVerId = convertObjectIdToVersionedId(SW_INSTALL_ID, client);
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(swInstallVerId).timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, swInstallVerId));
    }

    private void executeSwUninstallForUpdate(LwM2mClient client) {
        log.trace("[{}] Execute SW (uninstall with params(\"1\") ) [{}]", client.getEndpoint(), SW_UN_INSTALL_ID);
        String swUnInstallVerId = convertObjectIdToVersionedId(SW_UN_INSTALL_ID, client);
        TbLwM2MExecuteRequest request = TbLwM2MExecuteRequest.builder().versionedId(swUnInstallVerId).params("1").timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendExecuteRequest(client, request, new TbLwM2MExecuteCallback(logService, client, swUnInstallVerId));
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

    private LwM2MClientFwOtaInfo getOrInitFwInfo(LwM2mClient client) {
        return this.fwStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            LwM2MClientFwOtaInfo info = otaInfoStore.getFw(endpoint);
            if (info == null) {
                var profile = clientContext.getProfile(client.getRegistration());
                info = new LwM2MClientFwOtaInfo(endpoint, profile.getClientLwM2mSettings().getFwUpdateResource(),
                        LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(profile.getClientLwM2mSettings().getFwUpdateStrategy()));
                update(info);
            }
            return info;
        });
    }

    private LwM2MClientSwOtaInfo getOrInitSwInfo(LwM2mClient client) {
        return this.swStates.computeIfAbsent(client.getEndpoint(), endpoint -> {
            LwM2MClientSwOtaInfo info = otaInfoStore.getSw(endpoint);
            if (info == null) {
                var profile = clientContext.getProfile(client.getRegistration());
                OtherConfiguration clientLwM2mSettings = profile == null ? new OtherConfiguration() : profile.getClientLwM2mSettings();
                info = new LwM2MClientSwOtaInfo(endpoint, clientLwM2mSettings.getSwUpdateResource(),
                        LwM2MSoftwareUpdateStrategy.fromStrategySwByCode(clientLwM2mSettings.getSwUpdateStrategy()));
                update(info);
            }
            return info;
        });
    }

    private void update(LwM2MClientFwOtaInfo info) {
        otaInfoStore.putFw(info);
    }

    private void update(LwM2MClientSwOtaInfo info) {
        otaInfoStore.putSw(info);
    }

    private void sendStateUpdateToTelemetry(LwM2mClient client, LwM2MClientOtaInfo<?, ?, ?> fwInfo, OtaPackageUpdateStatus status, String log) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        TransportProtos.KeyValueProto.Builder kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(getAttributeKey(fwInfo.getType(), STATE));
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(status.name());
        result.add(kvProto.build());
        kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(LOG_LWM2M_TELEMETRY);
        kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(log);
        result.add(kvProto.build());
        helper.sendParametersOnThingsboardTelemetry(result, client.getSession(), client.getKeyTsLatestMap());
    }

    /**
     * send to client: versionedId="/19/65533/0/0, value = FwOtaInfo in bas64 -> format json:
     * send to client: versionedId="/19/65534/0/0, value = SwOtaInfo in bas64 -> format json:
     * {"title":"BC68JAR01",
     *  "version":"A10",
     *  "checksum":"f2a08d4963e981c78f2a99f62d8439af4437a72ea7267a8c01d013c072c01ded",
     *  "fileSize":59832.
     *  "fileName" : "BC68JAR01A10_TO_BC68JAR01A09_09.bin" }
     * @param client
     * @param targetId
     * @param response
     * @param otaPackageId
     */
    private void sendInfoToObject19ForOta(LwM2mClient client, String targetId, TransportProtos.GetOtaPackageResponseMsg response, UUID otaPackageId) {
        log.trace("[{}] Current info ota toObject19ForOta [{}]", client.getEndpoint(), targetId);
        String targetIdVer = convertObjectIdToVersionedId(targetId, client);
        ObjectModel objectModel = client.getObjectModel(targetIdVer, modelProvider);
        if (objectModel != null) {
            try {
                if (client.getRegistration().getSupportedObject().get(19) != null) {
                    ObjectNode objectNodeInfoOta = JacksonUtil.newObjectNode();
                    byte[] firmwareChunk = otaPackageDataCache.get(otaPackageId.toString(), 0, 0);
                    String fileChecksumSHA256 = Hashing.sha256().hashBytes(firmwareChunk).toString();
                    objectNodeInfoOta.put(OTA_INFO_19_TITLE, response.getTitle());
                    objectNodeInfoOta.put(OTA_INFO_19_VERSION, response.getVersion());
                    objectNodeInfoOta.put(OTA_INFO_19_FILE_CHECKSUM256, fileChecksumSHA256);
                    objectNodeInfoOta.put(OTA_INFO_19_FILE_SIZE, firmwareChunk.length);
                    objectNodeInfoOta.put(OTA_INFO_19_FILE_NAME, response.getFileName());
                    String objectNodeInfoOtaStr = JacksonUtil.toString(objectNodeInfoOta);
                    assert objectNodeInfoOtaStr != null;
                    String objectNodeInfoOtaBase64 = Base64.getEncoder().encodeToString(objectNodeInfoOtaStr.getBytes());
                    LwM2mPath pathOtaInstance = new LwM2mPath(targetId);
                    if (client.getRegistration().getAvailableInstances().contains(pathOtaInstance)) {
                        String versionId = targetIdVer + "/0/0";
                        TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionId).value(objectNodeInfoOtaBase64).timeout(clientContext.getRequestTimeout(client)).build();
                        downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionId));
                    } else {
                        String valueResourcesStr = "{\"" + 0 + "\":{\"0\":\"" + objectNodeInfoOtaBase64 + "\"}}";
                        String valueStr = "{\"id\":\"" + targetIdVer + "\",\"value\":" + valueResourcesStr + "}";
                        RpcCreateRequest requestBody = JacksonUtil.fromString(valueStr, RpcCreateRequest.class);
                        assert requestBody != null;
                        TbLwM2MCreateRequest.TbLwM2MCreateRequestBuilder builder = TbLwM2MCreateRequest.builder().versionedId(targetIdVer);
                        builder.value(requestBody.getValue()).nodes(requestBody.getNodes()).timeout(clientContext.getRequestTimeout(client));
                        downlinkHandler.sendCreateRequest(client, builder.build(), new TbLwM2MCreateResponseCallback(uplinkHandler, logService, client, targetIdVer));
                    }
                } else {
                    String errorMsg = String.format("[%s], Failed to send Info Ota to objectInstance [%s]. The client does not have object 19.", client.getEndpoint(), targetId);
                    log.trace(errorMsg);
                    logService.log(client, errorMsg);
                }
            } catch (Exception e){
                log.error("", e);
            }
        }
    }

    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(FirmwareUpdateResult fwUpdateResult) {
        switch (fwUpdateResult) {
            case INITIAL:
                return Optional.empty();
            case UPDATE_SUCCESSFULLY:
                return Optional.of(UPDATED);
            case NOT_ENOUGH:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case INTEGRITY_CHECK_FAILURE:
            case UNSUPPORTED_TYPE:
            case INVALID_URI:
            case UPDATE_FAILED:
            case UNSUPPORTED_PROTOCOL:
                return Optional.of(FAILED);
            default:
                throw new CodecException("Invalid value stateFw %s for FirmwareUpdateStatus.", fwUpdateResult.name());
        }
    }

    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(FirmwareUpdateState firmwareUpdateState) {
        switch (firmwareUpdateState) {
            case IDLE:
                return Optional.empty();
            case DOWNLOADING:
                return Optional.of(DOWNLOADING);
            case DOWNLOADED:
                return Optional.of(DOWNLOADED);
            case UPDATING:
                return Optional.of(UPDATING);
            default:
                throw new CodecException("Invalid value stateFw %d for FirmwareUpdateStatus.", firmwareUpdateState);
        }
    }

    private static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(SoftwareUpdateState swUpdateState) {
        switch (swUpdateState) {
            case INITIAL:
                return Optional.empty();
            case DOWNLOAD_STARTED:
                return Optional.of(DOWNLOADING);
            case DOWNLOADED:
                return Optional.of(DOWNLOADING);
            case DELIVERED:
                return Optional.of(DOWNLOADED);
            case INSTALLED:
                return Optional.empty();
            default:
                throw new CodecException("Invalid value stateSw %d for SoftwareUpdateState.", swUpdateState);
        }
    }

    /**
     * FirmwareUpdateStatus {
     * DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED
     */
    public static Optional<OtaPackageUpdateStatus> toOtaPackageUpdateStatus(SoftwareUpdateResult softwareUpdateResult) {
        switch (softwareUpdateResult) {
            case INITIAL:
                return Optional.empty();
            case DOWNLOADING:
                return Optional.of(DOWNLOADING);
            case SUCCESSFULLY_INSTALLED:
                return Optional.of(UPDATED);
            case SUCCESSFULLY_DOWNLOADED_VERIFIED:
                return Optional.of(VERIFIED);
            case NOT_ENOUGH_STORAGE:
            case OUT_OFF_MEMORY:
            case CONNECTION_LOST:
            case PACKAGE_CHECK_FAILURE:
            case UNSUPPORTED_PACKAGE_TYPE:
            case INVALID_URI:
            case UPDATE_ERROR:
            case INSTALL_FAILURE:
            case UN_INSTALL_FAILURE:
                return Optional.of(FAILED);
            default:
                throw new CodecException("Invalid value stateFw %s for FirmwareUpdateStatus.", softwareUpdateResult.name());
        }
    }

}
