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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_SUCCESS_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.LC_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = LC_EVENT_TABLE_NAME)
@NoArgsConstructor
public class LifecycleEventEntity extends EventEntity<LifecycleEvent> implements BaseEntity<LifecycleEvent> {

    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    @Column(name = EVENT_SUCCESS_COLUMN_NAME)
    private boolean success;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public LifecycleEventEntity(LifecycleEvent event) {
        super(event);
        this.eventType = event.getLcEventType();
        this.success = event.isSuccess();
        this.error = event.getError();
    }

    @Override
    public LifecycleEvent toData() {
        return LifecycleEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .lcEventType(eventType)
                .success(success)
                .error(error)
                .build();
    }

}
