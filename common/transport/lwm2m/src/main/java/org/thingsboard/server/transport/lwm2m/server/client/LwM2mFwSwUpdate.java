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
import org.eclipse.leshan.core.request.ContentFormat;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.firmware.FirmwareUpdateStatus;
import org.thingsboard.server.transport.lwm2m.server.DefaultLwM2MTransportMsgHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_PATH_RESOURCE_NAME_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_PATH_RESOURCE_PACKAGE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_PATH_RESOURCE_RESULT_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_PATH_RESOURCE_STATE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FW_PATH_RESOURCE_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.OBSERVE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2mTypeOper.WRITE_REPLACE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_PATH_RESOURCE_NAME_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_PATH_RESOURCE_PACKAGE_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SW_PATH_RESOURCE_VER_ID;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromObjectIdToIdVer;

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
    @Setter
    private volatile boolean info = false;
    private final String type;
    private DefaultLwM2MTransportMsgHandler serviceImpl;

    @Getter
    LwM2mClient lwM2MClient;

    @Getter
    @Setter
    private final List<String> pendingInfoRequests;

    public LwM2mFwSwUpdate(LwM2mClient lwM2MClient, String type) {
        this.lwM2MClient = lwM2MClient;
        this.type = type;
        this.pendingInfoRequests = new CopyOnWriteArrayList<>();
    }

    public void initReadValue(DefaultLwM2MTransportMsgHandler serviceImpl, String pathIdVer) {
        if (this.serviceImpl == null)  this.serviceImpl = serviceImpl;
        if (pathIdVer != null) {
            this.pendingInfoRequests.remove(pathIdVer);
        }
        if (this.pendingInfoRequests.size() == 0) {
            this.info = false;
            if (this.type.equals(FirmwareType.FIRMWARE.name()) && conditionalFwUpdateStart()){
                this.updateFw();
            }
            else if (this.type.equals(FirmwareType.SOFTWARE.name()) && conditionalSwUpdateStart()) {
                updateSw();
            }
        }
    }

    private void updateFw() {
        if (this.type.equals(FirmwareType.FIRMWARE.name())) {
            int chunkSize = 0;
            int chunk = 0;
            byte[] firmwareChunk = this.serviceImpl.firmwareDataCache.get(this.currentId.toString(), chunkSize, chunk);
            String targetIdVer = convertPathFromObjectIdToIdVer(FW_PATH_RESOURCE_PACKAGE_ID, this.lwM2MClient.getRegistration());
            this.observeStateFwUpdate ();
            this.serviceImpl.lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(), targetIdVer, WRITE_REPLACE, ContentFormat.OPAQUE.getName(),
                    firmwareChunk, this.serviceImpl.config.getTimeout(), null);
            this.stateUpdate = FirmwareUpdateStatus.DOWNLOADING.name();
            log.warn("updateFirmwareClient [{}] [{}] [{}] [{}] [{}]",
                    lwM2MClient.getFwUpdate().getCurrentVersion(),
                    this.lwM2MClient.getResourceValue(null, FW_PATH_RESOURCE_VER_ID),
                    this.currentTitle,
                    this.lwM2MClient.getResourceValue(null, FW_PATH_RESOURCE_NAME_ID),
                    this.stateUpdate
            );
        }
    }

    private void updateSw() {
        int chunkSize = 0;
        int chunk = 0;
        byte[] softwareChunk = this.serviceImpl.firmwareDataCache.get(this.currentId.toString(), chunkSize, chunk);
        String targetIdVer = convertPathFromObjectIdToIdVer(SW_PATH_RESOURCE_PACKAGE_ID, this.lwM2MClient.getRegistration());

        this.serviceImpl.lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(), targetIdVer, WRITE_REPLACE, ContentFormat.OPAQUE.getName(),
                softwareChunk, this.serviceImpl.config.getTimeout(), null);
        this.stateUpdate = FirmwareUpdateStatus.DOWNLOADING.name();
        log.warn("updateSoftwareClient [{}] [{}] [{}] [{}] [{}]",
                lwM2MClient.getSwUpdate().getCurrentVersion(),
                this.lwM2MClient.getResourceValue(null, SW_PATH_RESOURCE_VER_ID),
                this.currentTitle,
                this.lwM2MClient.getResourceValue(null, SW_PATH_RESOURCE_NAME_ID),
                this.stateUpdate
        );
        log.warn("updateSoftwareClient");
    }

    /**
     * FW: start
     * Проверяем состояние State (5.3) и Update Result (5.5).
     * 1. Если Update Result > 1 (some errors) - Это означает что пред. апдейт не прошел.
     * - Запускаем апдейт в независимости от состяния прошивки и ее версии.
     * 2. Если Update Result = 1 (or -1) && State = 0 (or -1)  - Это означает что пред. апдейт прошел.
     * - Проверяем поменялась ли версия и запускаем новый апдейт.
     * Мониторим состояние Update Result и State и мапим его на наш enum (Failed or Updated) or may be (DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED)
     * + пишем лог (в телеметрию отдельным полем error) с подробным статусом.
     * @valerii.sosliuk Вопрос к клиенту - как будем реагировать на Failed update? Когда повторять операцию?
     * - На update reg?
     * - Или клиент должен послать комканду на рестарт девайса?
     * - или переодически?
     * отправили прошивку:
     * -- Observe "Update Result" id=5  && "State" id=3
     * --- "Update Result" id=5 value must be = 0
     * ---  "State" id=3  value must be > 0
     * ---  to telemetry - DOWNLOADING
     * "Update Result" id=5 value change > 1  "Firmware updated not successfully" отправили прошивку: telemetry - FAILED
     * "Update Result" id=5 value change  ==1 "State" id=3  value == 0  "Firmware updated  successfully" отправили прошивку: telemetry - UPDATED
     */
    private boolean conditionalFwUpdateStart() {
        Long state = (Long)this.lwM2MClient.getResourceValue(null, FW_PATH_RESOURCE_STATE_ID);
        Long updateResult = (Long)this.lwM2MClient.getResourceValue(null, FW_PATH_RESOURCE_STATE_ID);
            // #1/#2
        return updateResult > 1L || ((state == 0L || state == -1L) && (updateResult == 1L || updateResult == -1L) &&
                (this.currentVersion != null && !this.currentVersion.equals(this.lwM2MClient.getResourceValue(null, FW_PATH_RESOURCE_VER_ID)))
                || (this.currentTitle != null && !this.currentTitle.equals(this.lwM2MClient.getResourceValue(null, FW_PATH_RESOURCE_NAME_ID))));
    }

    private void observeStateFwUpdate () {
        this.serviceImpl.lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(),
                convertPathFromObjectIdToIdVer(FW_PATH_RESOURCE_STATE_ID, this.lwM2MClient.getRegistration()), OBSERVE,
                null, null, this.serviceImpl.config.getTimeout(), null);
        this.serviceImpl.lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(),
                convertPathFromObjectIdToIdVer(FW_PATH_RESOURCE_RESULT_ID, this.lwM2MClient.getRegistration()), OBSERVE,
                null, null, this.serviceImpl.config.getTimeout(), null);
        this.serviceImpl.lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(),
                convertPathFromObjectIdToIdVer(FW_PATH_RESOURCE_NAME_ID, this.lwM2MClient.getRegistration()), OBSERVE,
                null, null, this.serviceImpl.config.getTimeout(), null);
        this.serviceImpl.lwM2mTransportRequest.sendAllRequest(lwM2MClient.getRegistration(),
                convertPathFromObjectIdToIdVer(FW_PATH_RESOURCE_VER_ID, this.lwM2MClient.getRegistration()), OBSERVE,
                null, null, this.serviceImpl.config.getTimeout(), null);
    }

    private boolean conditionalSwUpdateStart() {
        return false;
    }
}
