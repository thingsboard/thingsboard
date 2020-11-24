/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/21/2017.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.API_USAGE_STATE_TABLE_NAME)
public class ApiUsageStateEntity extends BaseSqlEntity<ApiUsageState> implements BaseEntity<ApiUsageState> {

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
    @Column(name = ModelConstants.API_USAGE_STATE_EMAIL_EXEC_COLUMN)
    private ApiUsageStateValue emailExecState = ApiUsageStateValue.ENABLED;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.API_USAGE_STATE_SMS_EXEC_COLUMN)
    private ApiUsageStateValue smsExecState = ApiUsageStateValue.ENABLED;

    public ApiUsageStateEntity() {
    }

    public ApiUsageStateEntity(ApiUsageState ur) {
        if (ur.getId() != null) {
            this.setUuid(ur.getId().getId());
        }
        this.setCreatedTime(ur.getCreatedTime());
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
        this.emailExecState = ur.getEmailExecState();
        this.smsExecState = ur.getSmsExecState();
    }

    @Override
    public ApiUsageState toData() {
        ApiUsageState ur = new ApiUsageState(new ApiUsageStateId(this.getUuid()));
        ur.setCreatedTime(createdTime);
        if (tenantId != null) {
            ur.setTenantId(new TenantId(tenantId));
        }
        if (entityId != null) {
            ur.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        }
        ur.setTransportState(transportState);
        ur.setDbStorageState(dbStorageState);
        ur.setReExecState(reExecState);
        ur.setJsExecState(jsExecState);
        ur.setEmailExecState(emailExecState);
        ur.setSmsExecState(smsExecState);
        return ur;
    }

}
