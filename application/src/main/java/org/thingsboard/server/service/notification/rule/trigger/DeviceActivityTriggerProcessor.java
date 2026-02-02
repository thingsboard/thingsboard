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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.DeviceActivityNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig.DeviceEvent;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

@Service
@RequiredArgsConstructor
public class DeviceActivityTriggerProcessor implements NotificationRuleTriggerProcessor<DeviceActivityTrigger, DeviceActivityNotificationRuleTriggerConfig> {

    private final TbDeviceProfileCache deviceProfileCache;

    @Override
    public boolean matchesFilter(DeviceActivityTrigger trigger, DeviceActivityNotificationRuleTriggerConfig triggerConfig) {
        DeviceEvent event = trigger.isActive() ? DeviceEvent.ACTIVE : DeviceEvent.INACTIVE;
        if (!triggerConfig.getNotifyOn().contains(event)) {
            return false;
        }
        DeviceId deviceId = trigger.getDeviceId();
        if (CollectionUtils.isNotEmpty(triggerConfig.getDevices())) {
            return triggerConfig.getDevices().contains(deviceId.getId());
        } else if (CollectionUtils.isNotEmpty(triggerConfig.getDeviceProfiles())) {
            DeviceProfile deviceProfile = deviceProfileCache.get(TenantId.SYS_TENANT_ID, deviceId);
            return deviceProfile != null && triggerConfig.getDeviceProfiles().contains(deviceProfile.getUuidId());
        } else {
            return true;
        }
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(DeviceActivityTrigger trigger) {
        return DeviceActivityNotificationInfo.builder()
                .eventType(trigger.isActive() ? "active" : "inactive")
                .deviceId(trigger.getDeviceId().getId())
                .deviceName(trigger.getDeviceName())
                .deviceType(trigger.getDeviceType())
                .deviceLabel(trigger.getDeviceLabel())
                .deviceCustomerId(trigger.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.DEVICE_ACTIVITY;
    }

}
