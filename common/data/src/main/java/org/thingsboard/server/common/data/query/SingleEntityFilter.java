// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class SingleEntityFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.SINGLE_ENTITY;
    }

    private AliasEntityId singleEntity;

}
