/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.id.TenantId;

@ApiModel
public class EntitySubtype {

    private static final long serialVersionUID = 8057240243059922101L;

    private TenantId tenantId;
    private EntityType entityType;
    private String type;

    public EntitySubtype() {
        super();
    }

    public EntitySubtype(TenantId tenantId, EntityType entityType, String type) {
        this.tenantId = tenantId;
        this.entityType = entityType;
        this.type = type;
    }

    @ApiModelProperty(position = 1, value = "JSON object with Tenant Id.", readOnly = true)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "ENTITY_VIEW", readOnly = true)
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    @ApiModelProperty(position = 3, required = true, value = "string value representing entity view type", example = "new-entity-view-type", readOnly = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntitySubtype that = (EntitySubtype) o;

        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
        if (entityType != that.entityType) return false;
        return type != null ? type.equals(that.type) : that.type == null;

    }

    @Override
    public int hashCode() {
        int result = tenantId != null ? tenantId.hashCode() : 0;
        result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EntitySubtype{");
        sb.append("tenantId=").append(tenantId);
        sb.append(", entityType=").append(entityType);
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
