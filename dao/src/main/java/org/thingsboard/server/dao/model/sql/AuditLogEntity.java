/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_DATA_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_STATUS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_NAME_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AUDIT_LOG_TABLE_NAME)
public class AuditLogEntity extends BaseSqlEntity<AuditLog> implements BaseEntity<AuditLog> {

    @Column(name = AUDIT_LOG_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = AUDIT_LOG_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = AUDIT_LOG_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = AUDIT_LOG_ENTITY_NAME_PROPERTY)
    private String entityName;

    @Column(name = AUDIT_LOG_USER_ID_PROPERTY)
    private UUID userId;

    @Column(name = AUDIT_LOG_USER_NAME_PROPERTY)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ACTION_TYPE_PROPERTY)
    private ActionType actionType;

    @Convert(converter = JsonConverter.class)
    @Column(name = AUDIT_LOG_ACTION_DATA_PROPERTY)
    private JsonNode actionData;

    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ACTION_STATUS_PROPERTY)
    private ActionStatus actionStatus;

    @Column(name = AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY)
    private String actionFailureDetails;

    public AuditLogEntity() {
        super();
    }

    public AuditLogEntity(AuditLog auditLog) {
        if (auditLog.getId() != null) {
            this.setUuid(auditLog.getId().getId());
        }
        this.setCreatedTime(auditLog.getCreatedTime());
        if (auditLog.getTenantId() != null) {
            this.tenantId = auditLog.getTenantId().getId();
        }
        if (auditLog.getCustomerId() != null) {
            this.customerId = auditLog.getCustomerId().getId();
        }
        if (auditLog.getEntityId() != null) {
            this.entityId = auditLog.getEntityId().getId();
            this.entityType = auditLog.getEntityId().getEntityType();
        }
        if (auditLog.getUserId() != null) {
            this.userId = auditLog.getUserId().getId();
        }
        this.entityName = auditLog.getEntityName();
        this.userName = auditLog.getUserName();
        this.actionType = auditLog.getActionType();
        this.actionData = auditLog.getActionData();
        this.actionStatus = auditLog.getActionStatus();
        this.actionFailureDetails = auditLog.getActionFailureDetails();
    }

    @Override
    public AuditLog toData() {
        AuditLog auditLog = new AuditLog(new AuditLogId(this.getUuid()));
        auditLog.setCreatedTime(createdTime);
        if (tenantId != null) {
            auditLog.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            auditLog.setCustomerId(new CustomerId(customerId));
        }
        if (entityId != null) {
            auditLog.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType.name(), entityId));
        }
        if (userId != null) {
            auditLog.setUserId(new UserId(userId));
        }
        auditLog.setEntityName(this.entityName);
        auditLog.setUserName(this.userName);
        auditLog.setActionType(this.actionType);
        auditLog.setActionData(this.actionData);
        auditLog.setActionStatus(this.actionStatus);
        auditLog.setActionFailureDetails(this.actionFailureDetails);
        return auditLog;
    }

}
