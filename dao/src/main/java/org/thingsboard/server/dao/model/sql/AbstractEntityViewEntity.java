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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@Slf4j
public abstract class AbstractEntityViewEntity<T extends EntityView> extends BaseVersionedEntity<T> {

    @Column(name = ModelConstants.ENTITY_VIEW_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = ModelConstants.ENTITY_VIEW_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.ENTITY_VIEW_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ModelConstants.DEVICE_TYPE_PROPERTY)
    private String type;

    @Column(name = ModelConstants.ENTITY_VIEW_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.ENTITY_VIEW_KEYS_PROPERTY)
    private String keys;

    @Column(name = ModelConstants.ENTITY_VIEW_START_TS_PROPERTY)
    private long startTs;

    @Column(name = ModelConstants.ENTITY_VIEW_END_TS_PROPERTY)
    private long endTs;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractEntityViewEntity() {
        super();
    }

    public AbstractEntityViewEntity(T entityView) {
        super(entityView);
        if (entityView.getEntityId() != null) {
            this.entityId = entityView.getEntityId().getId();
            this.entityType = entityView.getEntityId().getEntityType();
        }
        if (entityView.getTenantId() != null) {
            this.tenantId = entityView.getTenantId().getId();
        }
        if (entityView.getCustomerId() != null) {
            this.customerId = entityView.getCustomerId().getId();
        }
        this.type = entityView.getType();
        this.name = entityView.getName();
        try {
            this.keys = JacksonUtil.toString(entityView.getKeys());
        } catch (IllegalArgumentException e) {
            log.error("Unable to serialize entity view keys!", e);
        }
        this.startTs = entityView.getStartTimeMs();
        this.endTs = entityView.getEndTimeMs();
        this.additionalInfo = entityView.getAdditionalInfo();
        if (entityView.getExternalId() != null) {
            this.externalId = entityView.getExternalId().getId();
        }
    }

    public AbstractEntityViewEntity(EntityViewEntity entityViewEntity) {
        super(entityViewEntity);
        this.entityId = entityViewEntity.getEntityId();
        this.entityType = entityViewEntity.getEntityType();
        this.tenantId = entityViewEntity.getTenantId();
        this.customerId = entityViewEntity.getCustomerId();
        this.type = entityViewEntity.getType();
        this.name = entityViewEntity.getName();
        this.keys = entityViewEntity.getKeys();
        this.startTs = entityViewEntity.getStartTs();
        this.endTs = entityViewEntity.getEndTs();
        this.additionalInfo = entityViewEntity.getAdditionalInfo();
        this.externalId = entityViewEntity.getExternalId();
    }

    protected EntityView toEntityView() {
        EntityView entityView = new EntityView(new EntityViewId(getUuid()));
        entityView.setCreatedTime(createdTime);
        entityView.setVersion(version);

        if (entityId != null) {
            entityView.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType.name(), entityId));
        }
        if (tenantId != null) {
            entityView.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            entityView.setCustomerId(new CustomerId(customerId));
        }
        entityView.setType(type);
        entityView.setName(name);
        try {
            entityView.setKeys(JacksonUtil.fromString(keys, TelemetryEntityView.class));
        } catch (IllegalArgumentException e) {
            log.error("Unable to read entity view keys!", e);
        }
        entityView.setStartTimeMs(startTs);
        entityView.setEndTimeMs(endTs);
        entityView.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            entityView.setExternalId(new EntityViewId(externalId));
        }
        return entityView;
    }

}
