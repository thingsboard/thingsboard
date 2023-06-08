/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.DeviceActivityNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityNotificationRuleTriggerConfig.DeviceEvent;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.notification.trigger.RuleEngineMsgTrigger;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeviceActivityTriggerProcessor implements RuleEngineMsgNotificationRuleTriggerProcessor<DeviceActivityNotificationRuleTriggerConfig> {

    private final TbDeviceProfileCache deviceProfileCache;

    @Override
    public boolean matchesFilter(RuleEngineMsgTrigger trigger, DeviceActivityNotificationRuleTriggerConfig triggerConfig) {
        if (trigger.getMsg().getOriginator().getEntityType() != EntityType.DEVICE) {
            return false;
        }
        DeviceEvent event = trigger.getMsg().getType().equals(DataConstants.ACTIVITY_EVENT) ? DeviceEvent.ACTIVE : DeviceEvent.INACTIVE;
        if (!triggerConfig.getNotifyOn().contains(event)) {
            return false;
        }
        DeviceId deviceId = (DeviceId) trigger.getMsg().getOriginator();
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
    public RuleOriginatedNotificationInfo constructNotificationInfo(RuleEngineMsgTrigger trigger) {
        TbMsg msg = trigger.getMsg();
        return DeviceActivityNotificationInfo.builder()
                .eventType(trigger.getMsg().getType().equals(DataConstants.ACTIVITY_EVENT) ? "active" : "inactive")
                .deviceId(msg.getOriginator().getId())
                .deviceName(msg.getMetaData().getValue("deviceName"))
                .deviceType(msg.getMetaData().getValue("deviceType"))
                .deviceLabel(msg.getMetaData().getValue("deviceLabel"))
                .deviceCustomerId(msg.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.DEVICE_ACTIVITY;
    }

    @Override
    public Set<String> getSupportedMsgTypes() {
        return Set.of(DataConstants.ACTIVITY_EVENT, DataConstants.INACTIVITY_EVENT);
    }

}
