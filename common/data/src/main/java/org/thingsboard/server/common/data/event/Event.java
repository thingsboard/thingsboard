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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Event extends BaseData<EventId> {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final String serviceId;

    public Event(TenantId tenantId, EntityId entityId, String serviceId) {
        super();
        this.tenantId = tenantId != null ? tenantId : TenantId.SYS_TENANT_ID;
        this.entityId = entityId;
        this.serviceId = serviceId;
    }

    public abstract EventType getType();

}
