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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.transport.resource.Resource;
import org.thingsboard.server.common.data.transport.resource.ResourceType;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_VALUE_COLUMN;

@Data
@Entity
@Table(name = RESOURCE_TABLE_NAME)
@IdClass(ResourceCompositeKey.class)
public class ResourceEntity implements ToData<Resource> {

    @Id
    @Column(name = RESOURCE_TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Id
    @Column(name = RESOURCE_TYPE_COLUMN)
    private String resourceType;

    @Id
    @Column(name = RESOURCE_ID_COLUMN)
    private String resourceId;

    @Column(name = RESOURCE_VALUE_COLUMN)
    private String value;

    public ResourceEntity() {
    }

    public ResourceEntity(Resource resource) {
        this.tenantId = resource.getTenantId().getId();
        this.resourceType = resource.getResourceType().name();
        this.resourceId = resource.getResourceId();
        this.value = resource.getValue();
    }

    @Override
    public Resource toData() {
        Resource resource = new Resource();
        resource.setTenantId(new TenantId(tenantId));
        resource.setResourceType(ResourceType.valueOf(resourceType));
        resource.setResourceId(resourceId);
        resource.setValue(value);
        return resource;
    }
}
