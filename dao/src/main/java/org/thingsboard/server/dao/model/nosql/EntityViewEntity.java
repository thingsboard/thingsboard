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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import java.io.IOException;
import java.util.UUID;

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
public class EntityViewEntity implements SearchTextEntity<EntityView> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = ModelConstants.ENTITY_VIEW_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @PartitionKey(value = 2)
    @Column(name = ModelConstants.ENTITY_VIEW_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 3)
    @Column(name = ModelConstants.ENTITY_VIEW_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ModelConstants.ENTITY_VIEW_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.ENTITY_VIEW_KEYS_PROPERTY)
    private String keys;

    @Column(name = ModelConstants.ENTITY_VIEW_TS_BEGIN_PROPERTY)
    private String tsBegin;

    @Column(name = ModelConstants.ENTITY_VIEW_TS_END_PROPERTY)
    private String tsEnd;

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
        this.name = entityView.getName();
        try {
            this.keys = mapper.writeValueAsString(entityView.getKeys());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.tsBegin = entityView.getStartTs() != 0L ? String.valueOf(entityView.getStartTs()) : "0";
        this.tsEnd = entityView.getEndTs() != 0L ? String.valueOf(entityView.getEndTs()) : "0";
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
        entityView.setName(name);
        try {
            entityView.setKeys(mapper.readValue(keys, TelemetryEntityView.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        entityView.setStartTs(Long.parseLong(tsBegin));
        entityView.setEndTs(Long.parseLong(tsEnd));
        entityView.setAdditionalInfo(additionalInfo);
        return entityView;
    }
}
