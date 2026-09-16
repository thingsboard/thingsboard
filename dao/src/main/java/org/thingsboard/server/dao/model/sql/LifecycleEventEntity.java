// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
