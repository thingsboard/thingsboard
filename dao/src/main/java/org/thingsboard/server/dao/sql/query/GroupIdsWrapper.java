// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.query;

import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GroupIdsWrapper {

    @Getter
    private Map<GroupIdPermKey, Set<EntityGroupId>> groupIdsMap;

    public GroupIdsWrapper(MergedGroupTypePermissionInfo readPermissions, MergedGroupTypePermissionInfo attrPermissions, MergedGroupTypePermissionInfo tsPermissions) {
        groupIdsMap = new HashMap<>();

        Set<EntityGroupId> readGroups = getGroupsSet(readPermissions);
        Set<EntityGroupId> attrGroups = getGroupsSet(attrPermissions);
        Set<EntityGroupId> tsGroups = getGroupsSet(tsPermissions);

        for (EntityGroupId entityGroupId : readGroups) {
            add(entityGroupId, true, attrGroups.contains(entityGroupId), tsGroups.contains(entityGroupId));
        }
        for (EntityGroupId entityGroupId : attrGroups) {
            add(entityGroupId, readGroups.contains(entityGroupId), true, tsGroups.contains(entityGroupId));
        }
        for (EntityGroupId entityGroupId : tsGroups) {
            add(entityGroupId, readGroups.contains(entityGroupId), attrGroups.contains(entityGroupId), true);
        }
    }

    private void add(EntityGroupId id, boolean readFlag, boolean attrFlag, boolean tsFlag) {
        GroupIdPermKey key = new GroupIdPermKey(readFlag, attrFlag, tsFlag);
        groupIdsMap.computeIfAbsent(key, tmp -> new HashSet<>()).add(id);
    }

    private Set<EntityGroupId> getGroupsSet(MergedGroupTypePermissionInfo readPermissions) {
        return readPermissions.getEntityGroupIds() == null ? Collections.emptySet() : new HashSet<>(readPermissions.getEntityGroupIds());
    }
}
