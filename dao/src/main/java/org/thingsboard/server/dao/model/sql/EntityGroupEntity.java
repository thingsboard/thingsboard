// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.ENTITY_GROUP_TABLE_NAME)
public class EntityGroupEntity extends AbstractEntityGroupEntity<EntityGroup> {

    @Transient
    private static final long serialVersionUID = 8050086409213322856L;

    public EntityGroupEntity() {
        super();
    }

    public EntityGroupEntity(EntityGroup entityGroup) {
        super(entityGroup);
    }

    @Override
    public EntityGroup toData() {
        return super.toEntityGroup();
    }

}
