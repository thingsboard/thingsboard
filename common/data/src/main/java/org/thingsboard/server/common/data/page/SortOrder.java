// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.page;

import lombok.Data;

@Data
public class SortOrder {

    private final String property;
    private final Direction direction;

    public SortOrder(String property) {
        this(property, Direction.ASC);
    }

    public SortOrder(String property, Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public static SortOrder of(String property, Direction direction) {
        return new SortOrder(property, direction);
    }

    public static enum Direction {
        ASC, DESC
    }

    public static final SortOrder BY_CREATED_TIME_DESC = new SortOrder("createdTime", Direction.DESC);

}
