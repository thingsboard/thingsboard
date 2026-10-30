// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.proxy.HibernateProxy;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Table(name = ModelConstants.EXTENSION_TABLE_NAME)
public class ExtensionEntity extends BaseVersionedEntity<Extension> {

    // Sorting goes through DaoUtil.toPageable into JPQL queries, so these are entity attribute names.
    public static final Set<String> ALLOWED_SORT_PROPERTIES = Collections.unmodifiableSet(
            new LinkedHashSet<>(List.of("createdTime", "name", "state"))
    );

    @Column(name = ModelConstants.EXTENSION_TENANT_ID_PROPERTY, nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(name = ModelConstants.EXTENSION_NAME_PROPERTY, nullable = false)
    private String name;

    @Column(name = ModelConstants.EXTENSION_SLUG_PROPERTY, nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.EXTENSION_STATE_PROPERTY)
    private ExtensionState state;

    @Type(JsonBinaryType.class)
    @Column(name = ModelConstants.EXTENSION_CONTAINER_SPEC_PROPERTY, columnDefinition = "JSONB")
    private ExtensionContainerSpec containerSpec;

    @Column(name = ModelConstants.EXTENSION_LAST_ERROR_PROPERTY)
    private String lastError;

    @Column(name = ModelConstants.EXTENSION_LAST_ERROR_AT_PROPERTY)
    private Long lastErrorAt;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY, columnDefinition = "UUID")
    private UUID externalId;

    public ExtensionEntity() {}

    public ExtensionEntity(Extension extension) {
        super(extension);
        this.tenantId = getTenantUuid(extension.getTenantId());
        this.name = extension.getName();
        this.slug = extension.getSlug();
        this.state = extension.getState();
        this.containerSpec = extension.getContainerSpec();
        this.lastError = extension.getLastError();
        this.lastErrorAt = extension.getLastErrorAt();
        this.additionalInfo = extension.getAdditionalInfo();
        this.externalId = getUuid(extension.getExternalId());
    }

    @Override
    public Extension toData() {
        Extension extension = new Extension(new ExtensionId(id));
        extension.setCreatedTime(createdTime);
        extension.setVersion(version);
        extension.setTenantId(TenantId.fromUUID(tenantId));
        extension.setName(name);
        extension.setSlug(slug);
        extension.setState(state);
        extension.setContainerSpec(containerSpec);
        extension.setLastError(lastError);
        extension.setLastErrorAt(lastErrorAt);
        extension.setAdditionalInfo(additionalInfo);
        extension.setExternalId(getEntityId(externalId, ExtensionId::new));
        return extension;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ExtensionEntity that = (ExtensionEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
