// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.OWNER_INFO_VIEW_IS_PUBLIC_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_PROPERTY;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.OWNER_INFO_VIEW_TABLE_NAME)
public class OwnerInfoEntity extends BaseSqlEntity<EntityInfo> {

    @Column(name = TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ENTITY_TYPE_COLUMN)
    private String entityType;

    @Column(name = NAME_PROPERTY)
    private String name;

    @Column(name = OWNER_INFO_VIEW_IS_PUBLIC_PROPERTY)
    private boolean isPublic;

    public OwnerInfoEntity() {
        super();
    }

    @Override
    public EntityInfo toData() {
        return new EntityInfo(this.id, this.entityType, this.name);
    }
}
