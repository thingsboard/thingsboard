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

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TenantEntitiesDeletionHousekeeperTask extends HousekeeperTask {

    @Serial
    private static final long serialVersionUID = -8033108795318393447L;

    private EntityType entityType;

    public TenantEntitiesDeletionHousekeeperTask(TenantId tenantId, EntityType entityType) {
        super(tenantId, tenantId, HousekeeperTaskType.DELETE_TENANT_ENTITIES);
        this.entityType = entityType;
    }

    @JsonIgnore
    @Override
    public String getDescription() {
        return entityType.getNormalName().toLowerCase() + "s deletion";
    }

}
