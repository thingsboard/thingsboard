/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.DeviceInactivityNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceInactivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

@Service
@RequiredArgsConstructor
public class DeviceInactivityNotificationRuleTriggerProcessor implements NotificationRuleTriggerProcessor<TbMsg, DeviceInactivityNotificationRuleTriggerConfig> {

    private final TbDeviceProfileCache deviceProfileCache;

    @Override
    public boolean matchesFilter(TbMsg ruleEngineMsg, DeviceInactivityNotificationRuleTriggerConfig triggerConfig) {
        DeviceId deviceId = (DeviceId) ruleEngineMsg.getOriginator();
        if (CollectionUtils.isNotEmpty(triggerConfig.getDevices())) {
            return triggerConfig.getDevices().contains(deviceId);
        } else if (CollectionUtils.isNotEmpty(triggerConfig.getDeviceProfiles())) {
            DeviceProfile deviceProfile = deviceProfileCache.get(TenantId.SYS_TENANT_ID, deviceId);
            return deviceProfile != null && triggerConfig.getDeviceProfiles().contains(deviceProfile.getId());
        } else {
            return true;
        }
    }

    @Override
    public NotificationInfo constructNotificationInfo(TbMsg ruleEngineMsg, DeviceInactivityNotificationRuleTriggerConfig triggerConfig) {
        return DeviceInactivityNotificationInfo.builder()
                .deviceId(ruleEngineMsg.getOriginator().getId())
                .deviceName(ruleEngineMsg.getMetaData().getValue("deviceName"))
                .deviceType(ruleEngineMsg.getMetaData().getValue("deviceType"))
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.DEVICE_INACTIVITY;
    }

}
