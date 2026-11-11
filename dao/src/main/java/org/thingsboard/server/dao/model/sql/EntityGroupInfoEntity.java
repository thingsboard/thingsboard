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
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonType;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Set;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.ENTITY_GROUP_INFO_VIEW_TABLE_NAME)
public class EntityGroupInfoEntity extends AbstractEntityGroupEntity<EntityGroupInfo> {

    @JdbcType(PostgreSQLJsonPGObjectJsonType.class)
    @Column(name = ModelConstants.OWNER_IDS_COLUMN)
    private Set<EntityId> ownerIds;

    public EntityGroupInfoEntity() {
        super();
    }

    @Override
    public EntityGroupInfo toData() {
        return new EntityGroupInfo(super.toEntityGroup(), this.ownerIds);
    }

}
