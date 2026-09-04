// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;

@Data
public class EntityListFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_LIST;
    }

    private EntityType entityType;

    private List<String> entityList;

}
