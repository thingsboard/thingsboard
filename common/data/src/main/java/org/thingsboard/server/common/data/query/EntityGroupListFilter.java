// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;

@Schema
@Data
public class EntityGroupListFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ENTITY_GROUP_LIST;
    }

    private EntityType groupType;

    private List<String> entityGroupList;

}
