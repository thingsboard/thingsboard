/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Slf4j
@Service
@Primary
@TbCoreComponent
@RequiredArgsConstructor
public class LocalRuleEngineDeviceStateManager implements RuleEngineDeviceStateManager {

    private final DeviceStateService deviceStateService;

    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        try {
            deviceStateService.onDeviceConnect(tenantId, deviceId, connectTime);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process device connect event. Connect time: [{}].", tenantId.getId(), deviceId.getId(), connectTime, e);
            callback.onFailure(e);
            return;
        }
        callback.onSuccess();
    }

    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        try {
            deviceStateService.onDeviceActivity(tenantId, deviceId, activityTime);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process device activity event. Activity time: [{}].", tenantId.getId(), deviceId.getId(), activityTime, e);
            callback.onFailure(e);
            return;
        }
        callback.onSuccess();
    }

    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        try {
            deviceStateService.onDeviceDisconnect(tenantId, deviceId, disconnectTime);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process device disconnect event. Disconnect time: [{}].", tenantId.getId(), deviceId.getId(), disconnectTime, e);
            callback.onFailure(e);
            return;
        }
        callback.onSuccess();
    }

    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        try {
            deviceStateService.onDeviceInactivity(tenantId, deviceId, inactivityTime);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process device inactivity event. Inactivity time: [{}].", tenantId.getId(), deviceId.getId(), inactivityTime, e);
            callback.onFailure(e);
            return;
        }
        callback.onSuccess();
    }

}
