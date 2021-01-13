/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityConfigId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class EntityConfig extends BaseData<EntityConfigId> implements HasId<EntityConfigId>,  HasTenantId, HasCustomerId  {

    private TenantId tenantId;
    private CustomerId customerId;
    private EntityId entityId;
    private Long version;

    private JsonNode configuration;
    private JsonNode additionalInfo;

    public EntityConfig() {}

    public EntityConfig(EntityConfigId id) {
        this.id = id;
    }

}
