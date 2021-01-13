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
package org.thingsboard.server.dao.entityconfig;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityConfig;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityConfigId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface EntityConfigService {

    EntityConfig saveEntityConfigForEntity(TenantId tenantId, EntityId entityId, JsonNode configuration);

    void deleteEntityConfigsByEntityId(TenantId tenantId, EntityId EntityId);

    EntityConfig saveEntityConfigForEntity(TenantId tenantId, EntityId entityId, JsonNode configuration, JsonNode additionalInfo);

    EntityConfig getLatestEntityConfigByEntityId(TenantId tenantId, EntityId entityId);

    EntityConfig getEntityConfigById(TenantId tenantId, EntityConfigId entityConfigId);

    EntityConfig restoreEntityConfig(TenantId tenantId, EntityId entityId, EntityConfigId entityConfigId, JsonNode additionalInfo);

    PageData<EntityConfig> getEntityConfigsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);
}
