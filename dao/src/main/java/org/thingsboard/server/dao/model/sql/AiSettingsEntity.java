/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;
import org.thingsboard.server.common.data.ai.AiSettings;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.AiProviderConfig;
import org.thingsboard.server.common.data.id.AiSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Table(name = ModelConstants.AI_SETTINGS_TABLE_NAME)
public class AiSettingsEntity extends BaseVersionedEntity<AiSettings> {

    @Column(name = ModelConstants.AI_SETTINGS_TENANT_ID_COLUMN_NAME, nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = ModelConstants.AI_SETTINGS_NAME_COLUMN_NAME, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AI_SETTINGS_PROVIDER_COLUMN_NAME, nullable = false)
    private AiProvider provider;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.AI_SETTINGS_PROVIDER_CONFIG_COLUMN_NAME, nullable = false, columnDefinition = "JSONB")
    private AiProviderConfig providerConfig;

    @Column(name = ModelConstants.AI_SETTINGS_MODEL_COLUMN_NAME, nullable = false)
    private String model;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.AI_SETTINGS_MODEL_CONFIG_COLUMN_NAME, columnDefinition = "JSONB")
    private AiModelConfig modelConfig;

    public AiSettingsEntity() {}

    public AiSettingsEntity(AiSettings aiSettings) {
        super(aiSettings);
        tenantId = getTenantUuid(aiSettings.getTenantId());
        name = aiSettings.getName();
        provider = aiSettings.getProvider();
        providerConfig = aiSettings.getProviderConfig();
        model = aiSettings.getModel();
        modelConfig = aiSettings.getModelConfig();
    }

    @Override
    public AiSettings toData() {
        var settings = new AiSettings(new AiSettingsId(id));
        settings.setCreatedTime(createdTime);
        settings.setVersion(version);
        settings.setTenantId(TenantId.fromUUID(tenantId));
        settings.setName(name);
        settings.setProvider(provider);
        settings.setProviderConfig(providerConfig);
        settings.setModel(model);
        settings.setModelConfig(modelConfig);
        return settings;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AiSettingsEntity that = (AiSettingsEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
