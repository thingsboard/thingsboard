// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.housekeeper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EntitiesCleanupHousekeeperTask extends HousekeeperTask {

    private EntityType entityType;

    public EntitiesCleanupHousekeeperTask(EntityType entityType) {
        super(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, HousekeeperTaskType.CLEANUP_ENTITIES);
        this.entityType = entityType;
    }

    @JsonIgnore
    @Override
    public String getDescription() {
        return entityType.getNormalName().toLowerCase() + "s cleanup";
    }

}
