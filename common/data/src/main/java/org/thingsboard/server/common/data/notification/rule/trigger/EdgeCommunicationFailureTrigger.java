// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serial;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class EdgeCommunicationFailureTrigger implements NotificationRuleTrigger {

    @Serial
    private static final long serialVersionUID = 2918443863787603524L;

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final EdgeId edgeId;
    private final String edgeName;
    private final String failureMsg;
    private final String error;

    @Override
    public DeduplicationStrategy getDeduplicationStrategy() {
        return DeduplicationStrategy.ALL;
    }

    @Override
    public String getDeduplicationKey() {
        return String.join(":", NotificationRuleTrigger.super.getDeduplicationKey(), error);
    }

    @Override
    public long getDefaultDeduplicationDuration() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.EDGE_COMMUNICATION_FAILURE;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return edgeId;
    }

}
