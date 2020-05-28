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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.IOException;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.DEVICE_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_TABLE_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
@Data
@Table(name = ENTITY_VIEW_TABLE_FAMILY_NAME)
@EqualsAndHashCode
@ToString
@Slf4j
public class EntityViewEntity implements SearchTextEntity<EntityView> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = ModelConstants.ENTITY_VIEW_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = ModelConstants.ENTITY_VIEW_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @PartitionKey(value = 3)
    @Column(name = DEVICE_TYPE_PROPERTY)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = ModelConstants.ENTITY_VIEW_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = ModelConstants.ENTITY_VIEW_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.ENTITY_VIEW_KEYS_PROPERTY)
    private String keys;

    @Column(name = ModelConstants.ENTITY_VIEW_START_TS_PROPERTY)
    private long startTs;

    @Column(name = ModelConstants.ENTITY_VIEW_END_TS_PROPERTY)
    private long endTs;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    private static final ObjectMapper mapper = new ObjectMapper();

    public EntityViewEntity() {
        super();
    }

    public EntityViewEntity(EntityView entityView) {
        if (entityView.getId() != null) {
            this.id = entityView.getId().getId();
        }
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
            this.keys = mapper.writeValueAsString(entityView.getKeys());
        } catch (IOException e) {
            log.error("Unable to serialize entity view keys!", e);
        }
        this.startTs = entityView.getStartTimeMs();
        this.endTs = entityView.getEndTimeMs();
        this.searchText = entityView.getSearchText();
        this.additionalInfo = entityView.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public EntityView toData() {
        EntityView entityView = new EntityView(new EntityViewId(id));
        entityView.setCreatedTime(UUIDs.unixTimestamp(id));
        if (entityId != null) {
            entityView.setEntityId(EntityIdFactory.getByTypeAndId(entityType.name(), entityId.toString()));
        }
        if (tenantId != null) {
            entityView.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            entityView.setCustomerId(new CustomerId(customerId));
        }
        entityView.setType(type);
        entityView.setName(name);
        try {
            entityView.setKeys(mapper.readValue(keys, TelemetryEntityView.class));
        } catch (IOException e) {
            log.error("Unable to read entity view keys!", e);
        }
        entityView.setStartTimeMs(startTs);
        entityView.setEndTimeMs(endTs);
        entityView.setAdditionalInfo(additionalInfo);
        return entityView;
    }

    @Override
    public UUID getUuid() {
        return getId();
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }
}
