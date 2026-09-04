// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serial;

@Data
@Builder
public class NewPlatformVersionTrigger implements NotificationRuleTrigger {

    @Serial
    private static final long serialVersionUID = 3298785969736390092L;

    private final UpdateMessage updateInfo;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.NEW_PLATFORM_VERSION;
    }

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
        return DeduplicationStrategy.ALL;
    }

    @Override
    public String getDeduplicationKey() {
        return String.join(":", NotificationRuleTrigger.super.getDeduplicationKey(),
                updateInfo.getCurrentVersion(), updateInfo.getLatestVersion());
    }

}
