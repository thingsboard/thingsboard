// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
