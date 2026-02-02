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
