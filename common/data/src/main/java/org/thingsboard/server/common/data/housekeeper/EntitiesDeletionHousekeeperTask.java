/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.housekeeper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.UUID;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EntitiesDeletionHousekeeperTask extends HousekeeperTask {

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
