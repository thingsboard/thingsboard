// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_CONFIGURATION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_OWNER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_OWNER_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractEntityGroupEntity<T extends EntityGroup> extends BaseVersionedEntity<T> {

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_GROUP_TYPE_PROPERTY)
    private EntityType type;

    @Column(name = ENTITY_GROUP_NAME_PROPERTY)
    private String name;

    @Column(name = ENTITY_GROUP_OWNER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_GROUP_OWNER_TYPE_PROPERTY)
    private EntityType ownerType;

    @Convert(converter = JsonConverter.class)
    @Column(name = ENTITY_GROUP_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Convert(converter = JsonConverter.class)
    @Column(name = ENTITY_GROUP_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractEntityGroupEntity() {
        super();
    }

    public AbstractEntityGroupEntity(T entityGroup) {
        super(entityGroup);
        this.name = entityGroup.getName();
        this.type = entityGroup.getType();
        if (entityGroup.getOwnerId() != null) {
            this.ownerId = entityGroup.getOwnerId().getId();
            this.ownerType = entityGroup.getOwnerId().getEntityType();
        }
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
        if (entityGroup.getExternalId() != null) {
            this.externalId = entityGroup.getExternalId().getId();
        }
    }

    public AbstractEntityGroupEntity(EntityGroupEntity entityGroupEntity) {
        super(entityGroupEntity);
        this.name = entityGroupEntity.getName();
        this.type = entityGroupEntity.getType();
        this.ownerId = entityGroupEntity.getOwnerId();
        this.ownerType = entityGroupEntity.getOwnerType();
        this.additionalInfo = entityGroupEntity.getAdditionalInfo();
        this.configuration = entityGroupEntity.getConfiguration();
        this.externalId = entityGroupEntity.getExternalId();
    }

    protected EntityGroup toEntityGroup() {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(getUuid()));
        entityGroup.setCreatedTime(createdTime);
        entityGroup.setVersion(version);
        entityGroup.setName(name);
        entityGroup.setType(type);
        if (ownerId != null) {
            entityGroup.setOwnerId(EntityIdFactory.getByTypeAndUuid(ownerType, ownerId));
        }
        entityGroup.setAdditionalInfo(additionalInfo);
        entityGroup.setConfiguration(configuration);
        if (externalId != null) {
            entityGroup.setExternalId(new EntityGroupId(externalId));
        }
        return entityGroup;
    }

}
