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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityConfig;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityConfigId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_CONFIG_VERSION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_PROPERTY;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.ENTITY_CONFIG_COLUMN_FAMILY_NAME)
public class EntityConfigEntity extends BaseSqlEntity<EntityConfig> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Column(name = ModelConstants.TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.ENTITY_CONFIG_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = ENTITY_CONFIG_VERSION_PROPERTY)
    private Long version;

    @Type(type = "json")
    @Column(name = ModelConstants.ENTITY_CONFIG_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.DEVICE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;


    public EntityConfigEntity() {
    }

    public EntityConfigEntity(EntityConfig entityConfig) {
        if (entityConfig.getId() != null) {
            this.setUuid(entityConfig.getId().getId());
        }
        this.setCreatedTime(entityConfig.getCreatedTime());
        if (entityConfig.getTenantId() != null) {
            this.tenantId = entityConfig.getTenantId().getId();
        }
        this.entityId = entityConfig.getEntityId().getId();
        this.entityType = entityConfig.getEntityId().getEntityType();
        this.version = entityConfig.getVersion();
        this.configuration = entityConfig.getConfiguration();
        this.additionalInfo = entityConfig.getAdditionalInfo();
    }

    @Override
    public EntityConfig toData() {
        EntityConfig entityConfig = new EntityConfig(new EntityConfigId(this.getUuid()));
        entityConfig.setCreatedTime(this.getCreatedTime());
        if (entityId != null) {
            entityConfig.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType.name(), entityId));
        }
        if (tenantId != null) {
            entityConfig.setTenantId(new TenantId(tenantId));
        }

        entityConfig.setVersion(version);
        entityConfig.setConfiguration(configuration);
        entityConfig.setAdditionalInfo(additionalInfo);
        return entityConfig;
    }
}
