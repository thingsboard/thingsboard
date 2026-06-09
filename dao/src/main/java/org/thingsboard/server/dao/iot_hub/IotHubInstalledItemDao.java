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
package org.thingsboard.server.dao.iot_hub;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IotHubInstalledItemDao extends Dao<IotHubInstalledItem> {

    PageData<IotHubInstalledItem> findByTenantId(TenantId tenantId, List<String> itemTypes, UUID itemId, PageLink pageLink);

    List<UUID> findInstalledItemIdsByTenantId(TenantId tenantId);

    long countByTenantId(TenantId tenantId, String itemType);

    Map<UUID, Long> findInstalledItemCounts(TenantId tenantId, String itemType);

    void deleteByTenantId(TenantId tenantId);

}
