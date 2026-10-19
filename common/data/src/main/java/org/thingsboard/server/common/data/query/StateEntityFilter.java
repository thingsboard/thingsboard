// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

@Data
public class StateEntityFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.STATE_ENTITY;
    }

    private AliasEntityId defaultStateEntity;

}
