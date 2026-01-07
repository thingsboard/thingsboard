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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.PUBLIC_RESOURCE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_DESCRIPTOR_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_ETAG_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_FILE_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_IS_PUBLIC_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_SUB_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TITLE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RESOURCE_TABLE_NAME)
public class TbResourceInfoEntity extends BaseSqlEntity<TbResourceInfo> implements BaseEntity<TbResourceInfo> {

    @Column(name = RESOURCE_TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = RESOURCE_TITLE_COLUMN)
    private String title;

    @Column(name = RESOURCE_TYPE_COLUMN)
    private String resourceType;

    @Column(name = RESOURCE_SUB_TYPE_COLUMN)
    private String resourceSubType;

    @Column(name = RESOURCE_KEY_COLUMN)
    private String resourceKey;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = RESOURCE_ETAG_COLUMN)
    private String etag;

    @Column(name = RESOURCE_FILE_NAME_COLUMN)
    private String fileName;

    @Convert(converter = JsonConverter.class)
    @Column(name = RESOURCE_DESCRIPTOR_COLUMN)
    private JsonNode descriptor;

    @Column(name = RESOURCE_IS_PUBLIC_COLUMN)
    private Boolean isPublic;

    @Column(name = PUBLIC_RESOURCE_KEY_COLUMN, unique = true, updatable = false)
    private String publicResourceKey;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public TbResourceInfoEntity() {
    }

    public TbResourceInfoEntity(TbResourceInfo resource) {
        if (resource.getId() != null) {
            this.id = resource.getId().getId();
        }
        this.createdTime = resource.getCreatedTime();
        this.tenantId = resource.getTenantId().getId();
        this.title = resource.getTitle();
        this.resourceType = resource.getResourceType().name();
        if (resource.getResourceSubType() != null) {
            this.resourceSubType = resource.getResourceSubType().name();
        }
        this.resourceKey = resource.getResourceKey();
        this.searchText = resource.getSearchText();
        this.etag = resource.getEtag();
        this.fileName = resource.getFileName();
        this.descriptor = resource.getDescriptor();
        this.isPublic = resource.isPublic();
        this.publicResourceKey = resource.getPublicResourceKey();
        this.externalId = getUuid(resource.getExternalId());
    }

    @Override
    public TbResourceInfo toData() {
        TbResourceInfo resource = new TbResourceInfo(new TbResourceId(id));
        resource.setCreatedTime(createdTime);
        resource.setTenantId(TenantId.fromUUID(tenantId));
        resource.setTitle(title);
        resource.setResourceType(ResourceType.valueOf(resourceType));
        resource.setResourceSubType(resourceSubType != null ? ResourceSubType.valueOf(resourceSubType) : null);
        resource.setResourceKey(resourceKey);
        resource.setSearchText(searchText);
        resource.setEtag(etag);
        resource.setFileName(fileName);
        resource.setDescriptor(descriptor);
        resource.setPublic(isPublic == null || isPublic);
        resource.setPublicResourceKey(publicResourceKey);
        resource.setExternalId(getEntityId(externalId, TbResourceId::new));
        return resource;
    }
}
