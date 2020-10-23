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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
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

    @Column(name = ModelConstants.API_USAGE_STATE_TRANSPORT_ENABLED_COLUMN)
    private boolean transportEnabled = true;
    @Column(name = ModelConstants.API_USAGE_STATE_DB_STORAGE_ENABLED_COLUMN)
    private boolean dbStorageEnabled = true;
    @Column(name = ModelConstants.API_USAGE_STATE_RE_EXEC_ENABLED_COLUMN)
    private boolean reExecEnabled = true;
    @Column(name = ModelConstants.API_USAGE_STATE_JS_EXEC_ENABLED_COLUMN)
    private boolean jsExecEnabled = true;

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
        this.transportEnabled = ur.isTransportEnabled();
        this.dbStorageEnabled = ur.isDbStorageEnabled();
        this.reExecEnabled = ur.isReExecEnabled();
        this.jsExecEnabled = ur.isJsExecEnabled();
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
        ur.setTransportEnabled(transportEnabled);
        ur.setDbStorageEnabled(dbStorageEnabled);
        ur.setReExecEnabled(reExecEnabled);
        ur.setJsExecEnabled(jsExecEnabled);
        return ur;
    }

}
