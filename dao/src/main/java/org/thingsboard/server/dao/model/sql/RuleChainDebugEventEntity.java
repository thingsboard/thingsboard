// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_DEBUG_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RULE_CHAIN_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RuleChainDebugEventEntity extends EventEntity<RuleChainDebugEvent> implements BaseEntity<RuleChainDebugEvent> {

    @Column(name = EVENT_MESSAGE_COLUMN_NAME)
    private String message;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public RuleChainDebugEventEntity(RuleChainDebugEvent event) {
        super(event);
        this.message = event.getMessage();
        this.error = event.getError();
    }

    @Override
    public RuleChainDebugEvent toData() {
        return RuleChainDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .message(message)
                .error(error).build();
    }

}
