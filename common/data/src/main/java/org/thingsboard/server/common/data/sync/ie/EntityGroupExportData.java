// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.permission.GroupPermission;

import java.util.List;
import java.util.UUID;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EntityGroupExportData extends EntityExportData<EntityGroup> {

    @Schema(description = "Group permissions to apply to this group on import. " +
            "Meaningful only for USER groups; ignored for groups of any other type. " +
            "Each entry's userGroupId, roleId, and entityGroupId may use the external IDs of other entities in this payload " +
            "or the IDs of entities that already exist on the target tenant; the importer resolves them against the target tenant. " +
            "System-tenant roles are not allowed and will be rejected. " +
            "Leave null to skip permission management for this group.")
    private List<GroupPermission> permissions;

    @Schema(description = "OTA package assignments to apply to this group on import. " +
            "Meaningful only for DEVICE groups; ignored for groups of any other type. " +
            "Each entry's otaPackageId and groupId may reference external IDs of entities in this payload " +
            "or IDs of entities that already exist on the target tenant. " +
            "Leave null to skip OTA assignment management for this group.")
    private List<DeviceGroupOtaPackage> groupOtaPackages;

    @Schema(description = "Marker indicating that the group's member entities are intended to be transported alongside this payload. " +
            "Used by flows that convey members through a side channel (notably the version control flow, which stores members in a separate git index). " +
            "The solution import API does not consume this flag and does not require it to be set. " +
            "Safe to leave false (default).")
    private boolean groupEntities;

    @Schema(description = "External IDs of the entities that should be members of this group after import. " +
            "Each ID is resolved against the target tenant — by other entity in this payload, by external ID, or by existing internal ID — " +
            "and the matching entities are added to the group. " +
            "The import fails if any listed member cannot be resolved. " +
            "Must be null for the special 'All' group (whose membership is implicit and managed by the platform). " +
            "Leave null to skip membership wiring; existing membership on the target tenant is left untouched.")
    private List<UUID> memberIds;

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

    @JsonIgnore
    public boolean hasPermissions() {
        return permissions != null;
    }

    @JsonIgnore
    public boolean hasGroupEntities() {
        return groupEntities;
    }

}
