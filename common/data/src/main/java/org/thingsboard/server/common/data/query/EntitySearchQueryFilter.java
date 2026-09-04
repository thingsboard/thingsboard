// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
public abstract class EntitySearchQueryFilter implements EntityFilter {

    private AliasEntityId rootEntity;
    private String relationType;
    private EntitySearchDirection direction;
    private int maxLevel;
    private boolean fetchLastLevelOnly;
    private boolean rootStateEntity;
    private AliasEntityId defaultStateEntity;

}
