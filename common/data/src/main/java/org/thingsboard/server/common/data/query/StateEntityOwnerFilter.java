// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class StateEntityOwnerFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.STATE_ENTITY_OWNER;
    }

    private AliasEntityId singleEntity;
    private AliasEntityId defaultStateEntity;

}
