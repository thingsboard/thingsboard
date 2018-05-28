/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.model;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.type.EntityTypeCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_SUBTYPE_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_SUBTYPE_ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_SUBTYPE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_SUBTYPE_TYPE_PROPERTY;

@Table(name = ENTITY_SUBTYPE_COLUMN_FAMILY_NAME)
public class EntitySubtypeEntity {

    @PartitionKey(value = 0)
    @Column(name = ENTITY_SUBTYPE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 1)
    @Column(name = ENTITY_SUBTYPE_ENTITY_TYPE_PROPERTY, codec = EntityTypeCodec.class)
    private EntityType entityType;

    @PartitionKey(value = 2)
    @Column(name = ENTITY_SUBTYPE_TYPE_PROPERTY)
    private String type;

    public EntitySubtypeEntity() {
        super();
    }

    public EntitySubtypeEntity(EntitySubtype entitySubtype) {
        this.tenantId = entitySubtype.getTenantId().getId();
        this.entityType = entitySubtype.getEntityType();
        this.type = entitySubtype.getType();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

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

        EntitySubtypeEntity that = (EntitySubtypeEntity) o;

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
        final StringBuilder sb = new StringBuilder("EntitySubtypeEntity{");
        sb.append("tenantId=").append(tenantId);
        sb.append(", entityType=").append(entityType);
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public EntitySubtype toEntitySubtype() {
        EntitySubtype entitySubtype = new EntitySubtype();
        entitySubtype.setTenantId(new TenantId(tenantId));
        entitySubtype.setEntityType(entityType);
        entitySubtype.setType(type);
        return entitySubtype;
    }
}
