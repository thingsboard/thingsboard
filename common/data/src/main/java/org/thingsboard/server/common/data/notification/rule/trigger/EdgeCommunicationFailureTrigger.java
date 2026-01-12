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
