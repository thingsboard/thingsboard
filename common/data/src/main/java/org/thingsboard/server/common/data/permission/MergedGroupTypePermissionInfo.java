// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Schema
@Data
public class MergedGroupTypePermissionInfo {

    public static final MergedGroupTypePermissionInfo MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE =
            new MergedGroupTypePermissionInfo(Collections.emptyList(), true);
    public static final MergedGroupTypePermissionInfo MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_FALSE =
            new MergedGroupTypePermissionInfo(Collections.emptyList(), false);

    @Schema(description = "List of Entity Groups in case of group roles are assigned to the user (user group)")
    private final List<EntityGroupId> entityGroupIds; // immutable
    @Schema(description = "Indicates if generic permission assigned to the user group.")
    private final boolean hasGenericRead;

    public MergedGroupTypePermissionInfo(List<EntityGroupId> entityGroupIds, boolean hasGenericRead) {
        this.entityGroupIds = entityGroupIds == null ? null : List.copyOf(entityGroupIds);
        this.hasGenericRead = hasGenericRead;
    }

    public static MergedGroupTypePermissionInfo ofEmptyGroups(boolean hasGenericRead) {
        return hasGenericRead ? MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE : MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_FALSE;
    }

    public MergedGroupTypePermissionInfo addId(EntityGroupId id) {
        if (this.entityGroupIds == null || this.entityGroupIds.isEmpty()) {
            return new MergedGroupTypePermissionInfo(List.of(id), this.hasGenericRead);
        }
        List<EntityGroupId> mergedList = new ArrayList<>(this.entityGroupIds.size() + 1);
        mergedList.addAll(this.entityGroupIds);
        mergedList.add(id);
        return new MergedGroupTypePermissionInfo(mergedList, this.hasGenericRead);
    }

    public MergedGroupTypePermissionInfo addIds(List<EntityGroupId> ids) {
        if (this.entityGroupIds == null || this.entityGroupIds.isEmpty()) {
            return new MergedGroupTypePermissionInfo(new ArrayList<>(ids), this.hasGenericRead);
        }
        Set<EntityGroupId> result = new LinkedHashSet<>();
        result.addAll(this.entityGroupIds);
        result.addAll(ids);
        return new MergedGroupTypePermissionInfo(new ArrayList<>(result), this.hasGenericRead);
    }

}
