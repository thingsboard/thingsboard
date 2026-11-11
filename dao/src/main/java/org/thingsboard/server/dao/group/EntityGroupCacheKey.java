// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.group;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode
@RequiredArgsConstructor
public class EntityGroupCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = -3320634175313960334L;

    private final EntityId ownerId;
    private final EntityType entityType;
    private final String groupName;

    @Override
    public String toString() {
        return "{" + ownerId.getId() + "}_" + entityType + "_" + groupName;
    }

}
