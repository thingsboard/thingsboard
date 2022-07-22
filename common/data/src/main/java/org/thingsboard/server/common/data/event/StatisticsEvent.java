/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@ToString
@EqualsAndHashCode(callSuper = true)
public class StatisticsEvent extends Event {

    private static final long serialVersionUID = 6683733979448910631L;

    @Builder
    private StatisticsEvent(TenantId tenantId, EntityId entityId, String serviceId, long messagesProcessed, long errorsOccurred) {
        super(tenantId, entityId, serviceId);
        this.messagesProcessed = messagesProcessed;
        this.errorsOccurred = errorsOccurred;
    }

    @Getter
    private final long messagesProcessed;
    @Getter
    private final long errorsOccurred;

    @Override
    public EventType getType() {
        return EventType.STATS;
    }
}
