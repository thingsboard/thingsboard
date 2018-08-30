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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.ENTITY_VIEW_TABLE_FAMILY_NAME)
public class EntityViewEntity extends BaseSqlEntity<EntityView> implements SearchTextEntity<EntityView> {

    @Column(name = ModelConstants.ENTITY_VIEW_ENTITY_ID_PROPERTY)
    private String entityId;

    @Column(name = ModelConstants.ENTITY_VIEW_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.ENTITY_VIEW_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = ModelConstants.ENTITY_VIEW_NAME_PROPERTY)
    private String name;

    @Type(type = "json")
    @Column(name = ModelConstants.ENTITY_VIEW_KEYS_PROPERTY)
    private JsonNode keys;

    @Column(name = ModelConstants.ENTITY_VIEW_TS_BEGIN_PROPERTY)
    private String tsBegin;

    @Column(name = ModelConstants.ENTITY_VIEW_TS_END_PROPERTY)
    private String tsEnd;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public EntityViewEntity() {
        super();
    }

    public EntityViewEntity(EntityView entityView) {
        if (entityView.getId() != null) {
            this.setId(entityView.getId().getId());
        }
        if (entityView.getEntityId() != null) {
            this.entityId = toString(entityView.getEntityId().getId());
        }
        if (entityView.getTenantId() != null) {
            this.tenantId = toString(entityView.getTenantId().getId());
        }
        if (entityView.getCustomerId() != null) {
            this.customerId = toString(entityView.getCustomerId().getId());
        }
        this.name = entityView.getName();
        try {
            this.keys = new ObjectMapper().readTree("{\"" + entityView.getName() + "\" : [" +
                    entityView.getKeys().stream()
                            .map(k -> "\"" + k + "\"")
                            .collect(Collectors.joining(", ")) + "]}");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.tsBegin = String.valueOf(entityView.getTsBegin());
        this.tsEnd = String.valueOf(entityView.getTsEnd());
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
        EntityView entityView = new EntityView(new EntityViewId(getId()));
        entityView.setCreatedTime(UUIDs.unixTimestamp(getId()));

        /*Ned to refactor and replace DeviceId to Class<> instance*/
        entityView.setEntityId(entityId != null ? new DeviceId(toUUID(entityId)) : null);
        entityView.setTenantId(tenantId != null ? new TenantId(toUUID(tenantId)) : null);
        entityView.setCustomerId(customerId != null ? new CustomerId(toUUID(customerId)) : null);
        entityView.setName(name);
        try {
            entityView.setKeys(new ObjectMapper().readValue(keys.toString(), new TypeReference<List<String>>(){}));
        } catch (IOException e) {
            e.printStackTrace();
        }
        entityView.setAdditionalInfo(additionalInfo);
        return entityView;
    }
}
