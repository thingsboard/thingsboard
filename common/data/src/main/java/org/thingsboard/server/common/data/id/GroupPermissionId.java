// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema(allOf = EntityId.class)
public class GroupPermissionId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public GroupPermissionId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static GroupPermissionId fromString(String roleId) {
        return new GroupPermissionId(UUID.fromString(roleId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "GROUP_PERMISSION", allowableValues = "GROUP_PERMISSION")
    @Override
    public EntityType getEntityType() {
        return EntityType.GROUP_PERMISSION;
    }
}
