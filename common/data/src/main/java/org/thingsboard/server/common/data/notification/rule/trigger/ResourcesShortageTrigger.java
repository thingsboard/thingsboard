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
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serial;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class ResourcesShortageTrigger implements NotificationRuleTrigger {

    @Serial
    private static final long serialVersionUID = 6024216015202949570L;

    private Resource resource;
    private Long usage;
    private String serviceId;
    private String serviceType;

    @Override
    public TenantId getTenantId() {
        return TenantId.SYS_TENANT_ID;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return TenantId.SYS_TENANT_ID;
    }

    @Override
    public DeduplicationStrategy getDeduplicationStrategy() {
        return DeduplicationStrategy.ONLY_MATCHING;
    }

    @Override
    public String getDeduplicationKey() {
        return String.join(":", resource.name(), serviceId, serviceType);
    }

    @Override
    public long getDefaultDeduplicationDuration() {
        return TimeUnit.HOURS.toMillis(1);
    }

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.RESOURCES_SHORTAGE;
    }

    public enum Resource {
        CPU, RAM, STORAGE
    }

}
