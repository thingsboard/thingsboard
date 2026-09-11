// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule.trigger;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.io.Serializable;

public interface NotificationRuleTrigger extends Serializable {

    NotificationRuleTriggerType getType();

    TenantId getTenantId();

    EntityId getOriginatorEntityId();

    default DeduplicationStrategy getDeduplicationStrategy() {
        return DeduplicationStrategy.NONE;
    }

    default String getDeduplicationKey() {
        EntityId originatorEntityId = getOriginatorEntityId();
        return String.join(":", getType().toString(), originatorEntityId.getEntityType().toString(), originatorEntityId.getId().toString());
    }

    default long getDefaultDeduplicationDuration() {
        return 0;
    }

    enum DeduplicationStrategy {
        NONE,
        ALL,
        ONLY_MATCHING
    }

}
