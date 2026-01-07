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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_CONFIGURATION;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_CONFIGURATION_VERSION;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_ENTITY_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_ENTITY_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_VERSION;
import static org.thingsboard.server.dao.model.ModelConstants.DEBUG_SETTINGS;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CALCULATED_FIELD_TABLE_NAME)
public class CalculatedFieldEntity extends BaseVersionedEntity<CalculatedField> implements BaseEntity<CalculatedField> {

    @Column(name = CALCULATED_FIELD_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = CALCULATED_FIELD_ENTITY_TYPE)
    private String entityType;

    @Column(name = CALCULATED_FIELD_ENTITY_ID)
    private UUID entityId;

    @Column(name = CALCULATED_FIELD_TYPE)
    private String type;

    @Column(name = CALCULATED_FIELD_NAME)
    private String name;

    @Column(name = CALCULATED_FIELD_CONFIGURATION_VERSION)
    private int configurationVersion;

    @Convert(converter = JsonConverter.class)
    @Column(name = CALCULATED_FIELD_CONFIGURATION)
    private JsonNode configuration;

    @Column(name = CALCULATED_FIELD_VERSION)
    private Long version;

    @Column(name = DEBUG_SETTINGS)
    private String debugSettings;

    public CalculatedFieldEntity() {
        super();
    }

    public CalculatedFieldEntity(CalculatedField calculatedField) {
        this.setUuid(calculatedField.getUuidId());
        this.createdTime = calculatedField.getCreatedTime();
        this.tenantId = calculatedField.getTenantId().getId();
        this.entityType = calculatedField.getEntityId().getEntityType().name();
        this.entityId = calculatedField.getEntityId().getId();
        this.type = calculatedField.getType().name();
        this.name = calculatedField.getName();
        this.configurationVersion = calculatedField.getConfigurationVersion();
        this.configuration = JacksonUtil.valueToTree(calculatedField.getConfiguration());
        this.version = calculatedField.getVersion();
        this.debugSettings = JacksonUtil.toString(calculatedField.getDebugSettings());
    }

    @Override
    public CalculatedField toData() {
        CalculatedField calculatedField = new CalculatedField(new CalculatedFieldId(id));
        calculatedField.setCreatedTime(createdTime);
        calculatedField.setTenantId(TenantId.fromUUID(tenantId));
        calculatedField.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        calculatedField.setType(CalculatedFieldType.valueOf(type));
        calculatedField.setName(name);
        calculatedField.setConfigurationVersion(configurationVersion);
        calculatedField.setConfiguration(JacksonUtil.treeToValue(configuration, CalculatedFieldConfiguration.class));
        calculatedField.setVersion(version);
        calculatedField.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        return calculatedField;
    }

}
