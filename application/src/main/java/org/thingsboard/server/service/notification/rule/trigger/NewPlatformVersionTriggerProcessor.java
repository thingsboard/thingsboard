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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.NewPlatformVersionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NewPlatformVersionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NewPlatformVersionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;

@Service
@RequiredArgsConstructor
public class NewPlatformVersionTriggerProcessor implements NotificationRuleTriggerProcessor<NewPlatformVersionTrigger, NewPlatformVersionNotificationRuleTriggerConfig> {

    private final PartitionService partitionService;

    @Override
    public boolean matchesFilter(NewPlatformVersionTrigger trigger, NewPlatformVersionNotificationRuleTriggerConfig triggerConfig) {
         // todo: don't send repetitive notification after platform restart?
        if (!partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
            return false;
        }
        return trigger.getMessage().isUpdateAvailable();
    }

    @Override
    public NotificationInfo constructNotificationInfo(NewPlatformVersionTrigger trigger, NewPlatformVersionNotificationRuleTriggerConfig triggerConfig) {
        return NewPlatformVersionNotificationInfo.builder()
                .message(JacksonUtil.convertValue(trigger.getMessage(), new TypeReference<>() {}))
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.NEW_PLATFORM_VERSION;
    }

}
