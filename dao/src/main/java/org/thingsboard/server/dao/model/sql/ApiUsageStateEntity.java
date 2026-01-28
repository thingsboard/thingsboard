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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.API_USAGE_STATE_TABLE_NAME)
public class ApiUsageStateEntity extends BaseVersionedEntity<ApiUsageState> implements BaseEntity<ApiUsageState> {

    @Column(name = ModelConstants.API_USAGE_STATE_TENANT_ID_COLUMN)
    private UUID tenantId;
    @Column(name = ModelConstants.API_USAGE_STATE_ENTITY_TYPE_COLUMN)
    private String entityType;
    @Column(name = ModelConstants.API_USAGE_STATE_ENTITY_ID_COLUMN)
    private UUID entityId;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_TRANSPORT_COLUMN)
    private ApiUsageStateValue transportState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_DB_STORAGE_COLUMN)
    private ApiUsageStateValue dbStorageState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_RE_EXEC_COLUMN)
    private ApiUsageStateValue reExecState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_JS_EXEC_COLUMN)
    private ApiUsageStateValue jsExecState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_TBEL_EXEC_COLUMN)
    private ApiUsageStateValue tbelExecState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_EMAIL_EXEC_COLUMN)
    private ApiUsageStateValue emailExecState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_SMS_EXEC_COLUMN)
    private ApiUsageStateValue smsExecState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_ALARM_EXEC_COLUMN)
    private ApiUsageStateValue alarmExecState = ApiUsageStateValue.ENABLED;

    public ApiUsageStateEntity() {
    }

    public ApiUsageStateEntity(ApiUsageState ur) {
        super(ur);
        if (ur.getTenantId() != null) {
            this.tenantId = ur.getTenantId().getId();
        }
        if (ur.getEntityId() != null) {
            this.entityType = ur.getEntityId().getEntityType().name();
            this.entityId = ur.getEntityId().getId();
        }
        this.transportState = ur.getTransportState();
        this.dbStorageState = ur.getDbStorageState();
        this.reExecState = ur.getReExecState();
        this.jsExecState = ur.getJsExecState();
        this.tbelExecState = ur.getTbelExecState();
        this.emailExecState = ur.getEmailExecState();
        this.smsExecState = ur.getSmsExecState();
        this.alarmExecState = ur.getAlarmExecState();
    }

    @Override
    public ApiUsageState toData() {
        ApiUsageState ur = new ApiUsageState(new ApiUsageStateId(this.getUuid()));
        ur.setCreatedTime(createdTime);
        if (tenantId != null) {
            ur.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (entityId != null) {
            ur.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        }
        ur.setTransportState(transportState);
        ur.setDbStorageState(dbStorageState);
        ur.setReExecState(reExecState);
        ur.setJsExecState(jsExecState);
        ur.setTbelExecState(tbelExecState);
        ur.setEmailExecState(emailExecState);
        ur.setSmsExecState(smsExecState);
        ur.setAlarmExecState(alarmExecState);
        ur.setVersion(version);
        return ur;
    }

}
