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

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TenantEntitiesDeletionHousekeeperTask extends HousekeeperTask {

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
