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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.io.IOException;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.ENTITY_VIEW_TABLE_FAMILY_NAME)
@Slf4j
public class EntityViewEntity extends BaseSqlEntity<EntityView> implements SearchTextEntity<EntityView> {

    @Column(name = ModelConstants.ENTITY_VIEW_ENTITY_ID_PROPERTY)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = ModelConstants.ENTITY_VIEW_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.ENTITY_VIEW_CUSTOMER_ID_PROPERTY)
    private String customerId;

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
            this.setUuid(entityView.getId().getId());
        }
        if (entityView.getEntityId() != null) {
            this.entityId = toString(entityView.getEntityId().getId());
            this.entityType = entityView.getEntityId().getEntityType();
        }
        if (entityView.getTenantId() != null) {
            this.tenantId = toString(entityView.getTenantId().getId());
        }
        if (entityView.getCustomerId() != null) {
            this.customerId = toString(entityView.getCustomerId().getId());
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
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public EntityView toData() {
        EntityView entityView = new EntityView(new EntityViewId(this.getUuid()));
        entityView.setCreatedTime(UUIDs.unixTimestamp(this.getUuid()));

        if (entityId != null) {
            entityView.setEntityId(EntityIdFactory.getByTypeAndId(entityType.name(), toUUID(entityId).toString()));
        }
        if (tenantId != null) {
            entityView.setTenantId(new TenantId(toUUID(tenantId)));
        }
        if (customerId != null) {
            entityView.setCustomerId(new CustomerId(toUUID(customerId)));
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
}
