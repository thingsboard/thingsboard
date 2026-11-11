// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.Set;

@Data
public final class MergedGroupPermissionInfo {

    private final EntityType entityType;
    private final Set<Operation> operations;

    @JsonCreator
    public MergedGroupPermissionInfo(@JsonProperty("entityType") EntityType entityType, @JsonProperty("operations") Set<Operation> operations) {
        this.entityType = entityType;
        this.operations = operations;
    }

}
