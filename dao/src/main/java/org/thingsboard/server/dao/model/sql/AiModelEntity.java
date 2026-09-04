// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Table(name = ModelConstants.AI_MODEL_TABLE_NAME)
public class AiModelEntity extends BaseVersionedEntity<AiModel> {

    public static final Map<String, String> COLUMN_MAP = Map.of(
            "createdTime", "created_time",
            "provider", "(configuration ->> 'provider')",
            "modelId", "(configuration ->> 'modelId')"
    );

    public static final Set<String> ALLOWED_SORT_PROPERTIES = Collections.unmodifiableSet(
            new LinkedHashSet<>(List.of("createdTime", "name", "provider", "modelId"))
    );

    @Column(name = ModelConstants.AI_MODEL_TENANT_ID_COLUMN_NAME, nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = ModelConstants.AI_MODEL_NAME_COLUMN_NAME, nullable = false)
    private String name;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.AI_MODEL_CONFIGURATION_COLUMN_NAME, nullable = false, columnDefinition = "JSONB")
    private AiModelConfig configuration;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY, columnDefinition = "UUID")
    private UUID externalId;

    public AiModelEntity() {}

    public AiModelEntity(AiModel aiModel) {
        super(aiModel);
        tenantId = getTenantUuid(aiModel.getTenantId());
        name = aiModel.getName();
        configuration = aiModel.getConfiguration();
        externalId = getUuid(aiModel.getExternalId());
    }

    @Override
    public AiModel toData() {
        var model = new AiModel(new AiModelId(id));
        model.setCreatedTime(createdTime);
        model.setVersion(version);
        model.setTenantId(TenantId.fromUUID(tenantId));
        model.setName(name);
        model.setConfiguration(configuration);
        model.setExternalId(getEntityId(externalId, AiModelId::new));
        return model;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AiModelEntity that = (AiModelEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
