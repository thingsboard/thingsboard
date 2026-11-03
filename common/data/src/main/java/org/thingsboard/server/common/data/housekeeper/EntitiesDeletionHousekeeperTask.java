// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.housekeeper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.util.List;
import java.util.UUID;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EntitiesDeletionHousekeeperTask extends HousekeeperTask {

    @Serial
    private static final long serialVersionUID = 9009068831061529286L;

    private EntityType entityType;
    private List<UUID> entities;

    public EntitiesDeletionHousekeeperTask(TenantId tenantId, EntityType entityType, List<UUID> entities) {
        super(tenantId, tenantId, HousekeeperTaskType.DELETE_ENTITIES);
        this.entityType = entityType;
        this.entities = entities;
    }

    @JsonIgnore
    @Override
    public String getDescription() {
        return entityType.getNormalName().toLowerCase() + "s deletion (" + entities + ")";
    }

}
