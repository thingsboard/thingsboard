/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFiledLinkConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_CALCULATED_FIELD_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_CONFIGURATION;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_ENTITY_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_ENTITY_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_LINK_TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CALCULATED_FIELD_LINK_TABLE_NAME)
public class CalculatedFieldLinkEntity extends BaseSqlEntity<CalculatedFieldLink> implements BaseEntity<CalculatedFieldLink> {

    @Column(name = CALCULATED_FIELD_LINK_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = CALCULATED_FIELD_LINK_ENTITY_TYPE)
    private EntityType entityType;

    @Column(name = CALCULATED_FIELD_LINK_ENTITY_ID)
    private UUID entityId;

    @Column(name = CALCULATED_FIELD_LINK_CALCULATED_FIELD_ID)
    private UUID calculatedFieldId;

    @Convert(converter = JsonConverter.class)
    @Column(name = CALCULATED_FIELD_LINK_CONFIGURATION)
    private JsonNode configuration;

    public CalculatedFieldLinkEntity() {
        super();
    }

    public CalculatedFieldLinkEntity(CalculatedFieldLink calculatedFieldLink) {
        this.setUuid(calculatedFieldLink.getUuidId());
        this.createdTime = calculatedFieldLink.getCreatedTime();
        this.tenantId = calculatedFieldLink.getTenantId().getId();
        this.entityType = calculatedFieldLink.getEntityId().getEntityType();
        this.entityId = calculatedFieldLink.getEntityId().getId();
        this.calculatedFieldId = calculatedFieldLink.getCalculatedFieldId().getId();
        this.configuration = JacksonUtil.valueToTree(calculatedFieldLink.getConfiguration());
    }

    @Override
    public CalculatedFieldLink toData() {
        CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink(new CalculatedFieldLinkId(id));
        calculatedFieldLink.setCreatedTime(createdTime);
        calculatedFieldLink.setTenantId(TenantId.fromUUID(tenantId));
        calculatedFieldLink.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        calculatedFieldLink.setCalculatedFieldId(new CalculatedFieldId(calculatedFieldId));
        calculatedFieldLink.setConfiguration(JacksonUtil.treeToValue(configuration, CalculatedFiledLinkConfiguration.class));
        return calculatedFieldLink;
    }

}
