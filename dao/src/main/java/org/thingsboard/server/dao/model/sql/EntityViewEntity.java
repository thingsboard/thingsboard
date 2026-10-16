// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.ENTITY_VIEW_TABLE_NAME)
public class EntityViewEntity extends AbstractEntityViewEntity<EntityView> {

    public EntityViewEntity() {
        super();
    }

    public EntityViewEntity(EntityView entityView) {
        super(entityView);
    }

    @Override
    public EntityView toData() {
        return super.toEntityView();
    }
}
