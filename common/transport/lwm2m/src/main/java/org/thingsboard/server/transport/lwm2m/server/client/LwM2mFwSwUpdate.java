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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.request.ContentFormat;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.DefaultLwM2MTransportMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportRequest;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.thingsboard.server.common.data.ota.OtaPackageKey.STATE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getAttributeKey;
import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_NAME_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_PACKAGE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_UPDATE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_UPDATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.EXECUTE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_REPLACE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_INSTALL_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_NAME_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_PACKAGE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_UN_INSTALL_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_UPDATE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_UPDATE_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromObjectIdToIdVer;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.splitCamelCaseString;

@Slf4j
public class LwM2mFwSwUpdate {
    // 5/0/6 PkgName
    // 9/0/0 PkgName
    @Getter
    @Setter
    private volatile String currentTitle;
    // 5/0/7 PkgVersion
    // 9/0/1 PkgVersion
    @Getter
    @Setter
    private volatile String currentVersion;
    @Getter
    @Setter
    private volatile UUID currentId;
    @Getter
    @Setter
    private volatile String stateUpdate;
    @Getter
    private String pathPackageId;
    @Getter
    private String pathStateId;
    @Getter
    private String pathResultId;
    @Getter
    private String pathNameId;
    @Getter
    private String pathVerId;
    @Getter
    private String pathInstallId;
    @Getter
    private String pathUnInstallId;
    @Getter
    private String wUpdate;
    @Getter
    @Setter
    private volatile boolean infoFwSwUpdate = false;
    private final OtaPackageType type;
    @Getter
    LwM2mClient lwM2MClient;
    @Getter
    @Setter
    private final List<String> pendingInfoRequestsStart;
    @Getter
    @Setter
    private volatile Lwm2mClientRpcRequest rpcRequest;

    public LwM2mFwSwUpdate(LwM2mClient lwM2MClient, OtaPackageType type) {
        this.lwM2MClient = lwM2MClient;
        this.pendingInfoRequestsStart = new CopyOnWriteArrayList<>();
        this.type = type;
        this.stateUpdate = null;
        this.initPathId();
    }

    private void initPathId() {
        if (FIRMWARE.equals(this.type)) {
            this.pathPackageId = FW_PACKAGE_ID;
            this.pathStateId = FW_STATE_ID;
            this.pathResultId = FW_RESULT_ID;
            this.pathNameId = FW_NAME_ID;
            this.pathVerId = FW_VER_ID;
            this.pathInstallId = FW_UPDATE_ID;
            this.wUpdate = FW_UPDATE;
        } else if (SOFTWARE.equals(this.type)) {
            this.pathPackageId = SW_PACKAGE_ID;
            this.pathStateId = SW_UPDATE_STATE_ID;
            this.pathResultId = SW_RESULT_ID;
            this.pathNameId = SW_NAME_ID;
            this.pathVerId = SW_VER_ID;
            this.pathInstallId = SW_INSTALL_ID;
            this.pathUnInstallId = SW_UN_INSTALL_ID;
            this.wUpdate = SW_UPDATE;
        }
    }

    public void initReadValue(DefaultLwM2MTransportMsgHandler handler, LwM2mTransportRequest request, String pathIdVer) {
        if (pathIdVer != null) {
            this.pendingInfoRequestsStart.remove(pathIdVer);
        }
        if (this.pendingInfoRequestsStart.size() == 0) {
            this.infoFwSwUpdate = false;
            if (!OtaPackageUpdateStatus.DOWNLOADING.name().equals(this.stateUpdate)) {
                boolean conditionalStart = this.type.equals(FIRMWARE) ? this.conditionalFwUpdateStart() :
                        this.conditionalSwUpdateStart();
                if (conditionalStart) {
                    this.writeFwSwWare(handler, request);
                }
            }
        }
    }

    /**
     * Send FsSw to Lwm2mClient:
     * before operation Write: fw_state = DOWNLOADING
     */
    public void writeFwSwWare(DefaultLwM2MTransportMsgHandler handler, LwM2mTransportRequest request) {
        if (this.currentId != null) {
            this.stateUpdate = OtaPackageUpdateStatus.DOWNLOADING.name();
            this.sendLogs(handler, WRITE_REPLACE.name(), LOG_LW2M_INFO, null);
            int chunkSize = 0;
            int chunk = 0;
            byte[] firmwareChunk = handler.otaPackageDataCache.get(this.currentId.toString(), chunkSize, chunk);
            String targetIdVer = convertPathFromObjectIdToIdVer(this.pathPackageId, this.lwM2MClient.getRegistration());
            String fwMsg = String.format("%s: Start type operation %s paths:  %s", LOG_LW2M_INFO,
                    LwM2mTransportUtil.LwM2mTypeOper.FW_UPDATE.name(),  FW_PACKAGE_ID);
            handler.sendLogsToThingsboard(fwMsg, lwM2MClient.getRegistration().getId());
            log.warn("8) Start firmware Update. Send save to: [{}] ver: [{}] path: [{}]", this.lwM2MClient.getDeviceName(), this.currentVersion, targetIdVer);
            request.sendAllRequest(this.lwM2MClient.getRegistration(), targetIdVer, WRITE_REPLACE, ContentFormat.OPAQUE.getName(),
                    firmwareChunk, handler.config.getTimeout(), this.rpcRequest);
        }
        else {
            String msgError = "FirmWareId is null.";
            log.warn("6) [{}]", msgError);
            if (this.rpcRequest != null) {
                handler.sentRpcResponse(this.rpcRequest, CONTENT.name(), msgError, LOG_LW2M_ERROR);
            }
            log.error (msgError);
            this.sendLogs(handler, WRITE_REPLACE.name(), LOG_LW2M_ERROR, msgError);
        }
    }

    public void sendLogs(DefaultLwM2MTransportMsgHandler handler, String typeOper, String typeInfo, String msgError) {
        this.sendSateOnThingsBoard(handler);
        String msg = String.format("%s: %s, %s, pkgVer: %s: pkgName - %s state - %s.",
                typeInfo, this.wUpdate, typeOper, this.currentVersion, this.currentTitle, this.stateUpdate);
        if (LOG_LW2M_ERROR.equals(typeInfo)) {
            msg = String.format("%s Error: %s", msg, msgError);
        }
        handler.sendLogsToThingsboard(msg, lwM2MClient.getRegistration().getId());
    }


    /**
     * After inspection Update Result
     * fw_state/sw_state = UPDATING
     * send execute
     */
    public void executeFwSwWare(DefaultLwM2MTransportMsgHandler handler, LwM2mTransportRequest request) {
        this.setStateUpdate(UPDATING.name());
        this.sendLogs(handler, EXECUTE.name(), LOG_LW2M_INFO, null);
        request.sendAllRequest(this.lwM2MClient.getRegistration(), this.pathInstallId, EXECUTE, ContentFormat.TLV.getName(),
                null, 0, this.rpcRequest);
    }

    /**
     * Firmware start:
     * -- If the result of the update - errors (more than 1) - This means that the previous. the update failed.
     * - We launch the update regardless of the state of the firmware and its version.
     * -- If the result of the update is not errors (equal to 1 or 0) and ver is not empty - This means that before the update has passed.
     * -- If the result of the update is not errors and is empty - This means that there has not been an update yet.
     * - Check if the version has changed and launch a new update.
     */
    private boolean conditionalFwUpdateStart() {
        Long updateResultFw = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        // #1/#2
        return updateResultFw > LwM2mTransportUtil.UpdateResultFw.UPDATE_SUCCESSFULLY.code ||
                (
                        (updateResultFw <= LwM2mTransportUtil.UpdateResultFw.UPDATE_SUCCESSFULLY.code
                        ) &&
                                (
                                        (this.currentVersion != null && !this.currentVersion.equals(this.lwM2MClient.getResourceValue(null, this.pathVerId))) ||
                                                (this.currentTitle != null && !this.currentTitle.equals(this.lwM2MClient.getResourceValue(null, this.pathNameId)))
                                )
                );
    }


    /**
     * Before operation Execute  inspection Update Result :
     * 0 - Initial value
     */
    public boolean conditionalFwExecuteStart() {
        Long updateResult = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        return LwM2mTransportUtil.UpdateResultFw.INITIAL.code == updateResult;
    }

    /**
     * After operation Execute success  inspection Update Result :
     * 1 - "Firmware updated successfully"
     */
    public boolean conditionalFwExecuteAfterSuccess() {
        Long updateResult = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        return LwM2mTransportUtil.UpdateResultFw.UPDATE_SUCCESSFULLY.code == updateResult;
    }

    /**
     * After operation Execute success  inspection Update Result :
     *  > 1 error: "Firmware updated successfully"
     */
    public boolean conditionalFwExecuteAfterError() {
        Long updateResult = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        return LwM2mTransportUtil.UpdateResultFw.UPDATE_SUCCESSFULLY.code < updateResult;
    }

    /**
     * Software start
     * - If Update Result -errors (equal or more than 50) - This means that the previous. the update failed.
     * * - We launch the update regardless of the state of the firmware and its version.
     * - If Update Result is not errors (less than 50) and ver is not empty - This means that before. the update has passed.
     * - If Update Result is not errors and ver is empty - This means that there was no update yet or before. UnInstall update
     * - If Update Result is not errors and ver is not empty - This means that before unInstall update
     * * - Check if the version has changed and launch a new update.
     */
    private boolean conditionalSwUpdateStart() {
        Long updateResultSw = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        // #1/#2
        return updateResultSw >= LwM2mTransportUtil.UpdateResultSw.NOT_ENOUGH_STORAGE.code ||
                (
                        (updateResultSw <= LwM2mTransportUtil.UpdateResultSw.NOT_ENOUGH_STORAGE.code
                        ) &&
                                (
                                        (this.currentVersion != null && !this.currentVersion.equals(this.lwM2MClient.getResourceValue(null, this.pathVerId))) ||
                                                (this.currentTitle != null && !this.currentTitle.equals(this.lwM2MClient.getResourceValue(null, this.pathNameId)))
                                )
                );
    }

    /**
     * Before operation Execute inspection Update Result :
     * 3 - Successfully Downloaded and package integrity verified
     */
    public boolean conditionalSwUpdateExecute() {
        Long updateResult = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        return LwM2mTransportUtil.UpdateResultSw.SUCCESSFULLY_DOWNLOADED_VERIFIED.code == updateResult;
    }

    /**
     * After finish operation Execute (success):
     * -- inspection Update Result:
     * ---- FW если Update Result == 1 ("Firmware updated successfully") или  SW если Update Result == 2 ("Software successfully installed.")
     * -- fw_state/sw_state = UPDATED
     *
     * After finish operation Execute (error):
     * -- inspection updateResult and send to thingsboard info about error
     * --- send to telemetry ( key - this is name Update Result in model) (
     * --  fw_state/sw_state = FAILED
     */
    public void finishFwSwUpdate(DefaultLwM2MTransportMsgHandler handler, boolean success) {
        Long updateResult = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        String value = FIRMWARE.equals(this.type) ? LwM2mTransportUtil.UpdateResultFw.fromUpdateResultFwByCode(updateResult.intValue()).type :
                LwM2mTransportUtil.UpdateResultSw.fromUpdateResultSwByCode(updateResult.intValue()).type;
        String key = splitCamelCaseString((String) this.lwM2MClient.getResourceNameByRezId(null, this.pathResultId));
        if (success) {
            this.stateUpdate = OtaPackageUpdateStatus.UPDATED.name();
            this.sendLogs(handler, EXECUTE.name(), LOG_LW2M_INFO, null);
        } else {
            this.stateUpdate = OtaPackageUpdateStatus.FAILED.name();
            this.sendLogs(handler, EXECUTE.name(), LOG_LW2M_ERROR, value);
        }
        handler.helper.sendParametersOnThingsboardTelemetry(
                handler.helper.getKvStringtoThingsboard(key, value), this.lwM2MClient.getSession());
    }

    /**
     * After operation Execute success  inspection Update Result :
     * 2 - "Software successfully installed."
     */
    public boolean conditionalSwExecuteAfterSuccess() {
        Long updateResult = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        return LwM2mTransportUtil.UpdateResultSw.SUCCESSFULLY_INSTALLED.code == updateResult;
    }

    /**
     * After operation Execute success  inspection Update Result :
     *  >= 50 - error "NOT_ENOUGH_STORAGE"
     */
    public boolean conditionalSwExecuteAfterError() {
        Long updateResult = (Long) this.lwM2MClient.getResourceValue(null, this.pathResultId);
        return LwM2mTransportUtil.UpdateResultSw.NOT_ENOUGH_STORAGE.code <= updateResult;
    }

    private void observeStateUpdate(DefaultLwM2MTransportMsgHandler handler, LwM2mTransportRequest request) {
        request.sendAllRequest(lwM2MClient.getRegistration(),
                convertPathFromObjectIdToIdVer(this.pathStateId, this.lwM2MClient.getRegistration()), OBSERVE,
                null, null, 0, null);
        request.sendAllRequest(lwM2MClient.getRegistration(),
                convertPathFromObjectIdToIdVer(this.pathResultId, this.lwM2MClient.getRegistration()), OBSERVE,
                null, null, 0, null);
    }

    public void sendSateOnThingsBoard(DefaultLwM2MTransportMsgHandler handler) {
        if (StringUtils.trimToNull(this.stateUpdate) != null) {
            List<TransportProtos.KeyValueProto> result = new ArrayList<>();
            TransportProtos.KeyValueProto.Builder kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(getAttributeKey(this.type, STATE));
            kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV(stateUpdate);
            result.add(kvProto.build());
            handler.helper.sendParametersOnThingsboardTelemetry(result,
                    handler.getSessionInfoOrCloseSession(this.lwM2MClient.getRegistration()));
        }
    }

    public void sendReadObserveInfo(LwM2mTransportRequest request) {
        this.infoFwSwUpdate = true;
        this.pendingInfoRequestsStart.add(convertPathFromObjectIdToIdVer(
                this.pathVerId, this.lwM2MClient.getRegistration()));
        this.pendingInfoRequestsStart.add(convertPathFromObjectIdToIdVer(
                this.pathNameId, this.lwM2MClient.getRegistration()));
        this.pendingInfoRequestsStart.add(convertPathFromObjectIdToIdVer(
                this.pathStateId, this.lwM2MClient.getRegistration()));
        this.pendingInfoRequestsStart.add(convertPathFromObjectIdToIdVer(
                this.pathResultId, this.lwM2MClient.getRegistration()));
        this.pendingInfoRequestsStart.forEach(pathIdVer -> {
            request.sendAllRequest(this.lwM2MClient.getRegistration(), pathIdVer, OBSERVE, ContentFormat.TLV.getName(),
                    null, 0, this.rpcRequest);
        });

    }
}
