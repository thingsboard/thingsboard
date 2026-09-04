// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class EntityDataSortOrder {

    private EntityKey key;
    private Direction direction;

    public EntityDataSortOrder() {}

    public EntityDataSortOrder(EntityKey key) {
        this(key, Direction.ASC);
    }

    public EntityDataSortOrder(EntityKey key, Direction direction) {
        this.key = key;
        this.direction = direction;
    }

    public enum Direction {
        ASC, DESC
    }

}
