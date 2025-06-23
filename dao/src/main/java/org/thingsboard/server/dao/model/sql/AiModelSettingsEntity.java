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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;
import org.thingsboard.server.common.data.ai.AiModelSettings;
import org.thingsboard.server.common.data.ai.model.AiModel;
import org.thingsboard.server.common.data.id.AiModelSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Table(name = ModelConstants.AI_MODEL_SETTINGS_TABLE_NAME)
public class AiModelSettingsEntity extends BaseVersionedEntity<AiModelSettings> {

    public static final Map<String, String> COLUMN_MAP = Map.of(
            "createdTime", "created_time"
    );

    @Column(name = ModelConstants.AI_MODEL_SETTINGS_TENANT_ID_COLUMN_NAME, nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = ModelConstants.AI_MODEL_SETTINGS_NAME_COLUMN_NAME, nullable = false)
    private String name;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.AI_MODEL_SETTINGS_CONFIGURATION_COLUMN_NAME, nullable = false, columnDefinition = "JSONB")
    private AiModel<?> configuration;

    public AiModelSettingsEntity() {}

    public AiModelSettingsEntity(AiModelSettings aiModelSettings) {
        super(aiModelSettings);
        tenantId = getTenantUuid(aiModelSettings.getTenantId());
        name = aiModelSettings.getName();
        configuration = aiModelSettings.getConfiguration();
    }

    @Override
    public AiModelSettings toData() {
        var settings = new AiModelSettings(new AiModelSettingsId(id));
        settings.setCreatedTime(createdTime);
        settings.setVersion(version);
        settings.setTenantId(TenantId.fromUUID(tenantId));
        settings.setName(name);
        settings.setConfiguration(configuration);
        return settings;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AiModelSettingsEntity that = (AiModelSettingsEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
