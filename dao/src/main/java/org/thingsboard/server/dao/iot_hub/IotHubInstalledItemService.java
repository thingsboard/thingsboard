// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.iot_hub;

import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IotHubInstalledItemService {

    IotHubInstalledItem save(TenantId tenantId, IotHubInstalledItem item);

    IotHubInstalledItem findById(TenantId tenantId, IotHubInstalledItemId id);

    PageData<IotHubInstalledItem> findByTenantId(TenantId tenantId, List<String> itemTypes, UUID itemId, PageLink pageLink);

    List<UUID> findInstalledItemIdsByTenantId(TenantId tenantId);

    List<UUID> findInstalledItemIdsByTenantIdAndItemIdIn(TenantId tenantId, Collection<UUID> itemIds);

    long countByTenantId(TenantId tenantId, String itemType);

    Map<UUID, Long> findInstalledItemCounts(TenantId tenantId, String itemType);

    void deleteById(TenantId tenantId, IotHubInstalledItemId id);

    void deleteByTenantId(TenantId tenantId);

}
