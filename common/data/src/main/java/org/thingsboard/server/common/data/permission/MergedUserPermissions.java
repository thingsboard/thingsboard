// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Schema
public final class MergedUserPermissions implements Serializable {

    @Schema(description = "Map of permissions defined using generic roles ('Customer Administrator', etc)")
    private final Map<Resource, Set<Operation>> genericPermissions;

    @Schema(description = "Map of permissions defined using group roles ('Read' or 'Write' access to specific entity group, etc)")
    private final Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions;

    @Schema(description = "Map of read permissions per entity type. Used on the UI to enable/disable certain components.")
    private final Map<EntityType, MergedGroupTypePermissionInfo> readGroupPermissions;

    @Schema(description = "Map of read permissions per resource. Used on the UI to enable/disable certain components.")
    private final Map<Resource, MergedGroupTypePermissionInfo> readEntityPermissions;

    @Schema(description = "Map of read entity attributes permissions per resource. Used on the UI to enable/disable certain tabs.")
    private final Map<Resource, MergedGroupTypePermissionInfo> readAttrPermissions;
    @Schema(description = "Map of read entity time-series permissions per resource. Used on the UI to enable/disable certain tabs.")
    private final Map<Resource, MergedGroupTypePermissionInfo> readTsPermissions;

    public static final MergedUserPermissions ALL = new MergedUserPermissions(Map.of(Resource.ALL, Set.of(Operation.ALL)), Collections.emptyMap());

    @JsonCreator
    public MergedUserPermissions(@JsonProperty("genericPermissions") Map<Resource, Set<Operation>> genericPermissions,
                                 @JsonProperty("groupPermissions") Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions) {
        this.genericPermissions = genericPermissions;
        this.groupPermissions = groupPermissions;
        this.readGroupPermissions = new HashMap<>();
        this.readEntityPermissions = new HashMap<>();
        this.readAttrPermissions = new HashMap<>();
        this.readTsPermissions = new HashMap<>();
        for (EntityType groupType : EntityGroup.groupTypes) {
            Resource resource = Resource.groupResourceFromGroupType(groupType);
            boolean hasGenericRead = hasGenericPermission(resource, Operation.READ);
            this.readGroupPermissions.put(groupType, MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericRead));
            this.readEntityPermissions.put(resource, MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericRead));
            boolean hasGenericAttrRead = hasGenericPermission(resource, Operation.READ_ATTRIBUTES);
            this.readAttrPermissions.put(resource, MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericAttrRead));
            boolean hasGenericTsRead = hasGenericPermission(resource, Operation.READ_TELEMETRY);
            this.readTsPermissions.put(resource, MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericTsRead));
        }

        for (EntityType entityType : EntityType.values()) {
            Resource resource = Resource.resourceFromEntityType(entityType);
            if (resource != null) {
                boolean hasGenericRead = hasGenericPermission(resource, Operation.READ);
                this.readEntityPermissions.put(resource, MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericRead));
                boolean hasGenericReadAttributes = hasGenericPermission(resource, Operation.READ_ATTRIBUTES);
                this.readAttrPermissions.put(resource, MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericReadAttributes));
                boolean hasGenericReadTelemetry = hasGenericPermission(resource, Operation.READ_TELEMETRY);
                this.readTsPermissions.put(resource, MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericReadTelemetry));
            }
        }

        if (!this.groupPermissions.isEmpty()) {
            Map<EntityType, List<EntityGroupId>> readGroupPermissionIds = new HashMap<>();
            Map<Resource, List<EntityGroupId>> readEntityPermissionIds = new HashMap<>();
            Map<Resource, List<EntityGroupId>> readAttrPermissionIds = new HashMap<>();
            Map<Resource, List<EntityGroupId>> readTsPermissionIds = new HashMap<>();
            this.groupPermissions.forEach((id, info) -> {
                if (checkOperation(info.getOperations(), Operation.READ)) {
                    readGroupPermissionIds.computeIfAbsent(info.getEntityType(), et -> new ArrayList<>()).add(id);
                    Resource entityResource = Resource.resourceFromEntityType(info.getEntityType());
                    readEntityPermissionIds.computeIfAbsent(entityResource, et -> new ArrayList<>()).add(id);
                    Resource groupResource = Resource.groupResourceFromGroupType(info.getEntityType());
                    if (groupResource != null) {
                        readEntityPermissionIds.computeIfAbsent(groupResource, et -> new ArrayList<>()).add(id);
                    }
                }
                if (checkOperation(info.getOperations(), Operation.READ_ATTRIBUTES)) {
                    Resource entityResource = Resource.resourceFromEntityType(info.getEntityType());
                    readAttrPermissionIds.computeIfAbsent(entityResource, et -> new ArrayList<>()).add(id);
                    Resource groupResource = Resource.groupResourceFromGroupType(info.getEntityType());
                    if (groupResource != null) {
                        readAttrPermissionIds.computeIfAbsent(groupResource, et -> new ArrayList<>()).add(id);
                    }
                }
                if (checkOperation(info.getOperations(), Operation.READ_TELEMETRY)) {
                    Resource entityResource = Resource.resourceFromEntityType(info.getEntityType());
                    readTsPermissionIds.computeIfAbsent(entityResource, et -> new ArrayList<>()).add(id);
                    Resource groupResource = Resource.groupResourceFromGroupType(info.getEntityType());
                    if (groupResource != null) {
                        readTsPermissionIds.computeIfAbsent(groupResource, et -> new ArrayList<>()).add(id);
                    }
                }
            });
            addIds(this.readGroupPermissions, readGroupPermissionIds);
            addIds(this.readEntityPermissions, readEntityPermissionIds);
            addIds(this.readAttrPermissions, readAttrPermissionIds);
            addIds(this.readTsPermissions, readTsPermissionIds);
        }
    }

    <T> void addIds(Map<T, MergedGroupTypePermissionInfo> permissions, Map<T, List<EntityGroupId>> ids) {
        ids.forEach((key, value) -> {
            final MergedGroupTypePermissionInfo mergedGroupTypePermissionInfo = permissions.get(key);
            final MergedGroupTypePermissionInfo newInfo = mergedGroupTypePermissionInfo.addIds(value);
            permissions.put(key, newInfo);
        });
    }

    public MergedGroupTypePermissionInfo getGroupPermissionsByEntityTypeAndOperation(EntityType entityType, Operation operation) {
        Resource resource = Resource.resourceFromEntityType(entityType);
        boolean hasGenericPermission = hasGenericPermission(resource, operation);
        List<EntityGroupId> groupIds = this.groupPermissions.entrySet().stream().filter((entry) ->
                entry.getValue().getEntityType().equals(entityType) && checkOperation(entry.getValue().getOperations(), operation)
        ).map(Map.Entry::getKey).collect(Collectors.toList());
        return new MergedGroupTypePermissionInfo(groupIds, hasGenericPermission);
    }

    public boolean hasGenericPermission(Resource resource, Operation operation) {
        return hasGenericResourcePermission(resource, operation) || hasGenericAllPermission(operation);
    }

    private boolean hasGenericAllPermission(Operation operation) {
        Set<Operation> operations = genericPermissions.get(Resource.ALL);
        return operations != null && checkOperation(operations, operation);
    }

    private boolean hasGenericResourcePermission(Resource resource, Operation operation) {
        Set<Operation> operations = genericPermissions.get(resource);
        return operations != null && checkOperation(operations, operation);
    }

    private boolean checkOperation(Set<Operation> operations, Operation operation) {
        return operations.contains(Operation.ALL) || operations.contains(operation);
    }

    public boolean hasGroupPermissions(EntityGroupId entityGroupId, Operation operation) {
        MergedGroupPermissionInfo permissionInfo = groupPermissions.get(entityGroupId);
        return permissionInfo != null && checkOperation(permissionInfo.getOperations(), operation);
    }

}
