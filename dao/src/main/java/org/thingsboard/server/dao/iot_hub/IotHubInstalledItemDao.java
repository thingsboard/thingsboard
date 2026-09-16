// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.iot_hub;

import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IotHubInstalledItemDao extends Dao<IotHubInstalledItem> {

    Optional<IotHubInstalledItem> findByTenantIdAndId(TenantId tenantId, IotHubInstalledItemId installedItemId);

    PageData<IotHubInstalledItem> findByTenantId(TenantId tenantId, List<String> itemTypes, UUID itemId, PageLink pageLink);

    List<UUID> findInstalledItemIdsByTenantId(TenantId tenantId);

    List<UUID> findInstalledItemIdsByTenantIdAndItemIdIn(TenantId tenantId, Collection<UUID> itemIds);

    long countByTenantId(TenantId tenantId, String itemType);

    Map<UUID, Long> findInstalledItemCounts(TenantId tenantId, String itemType);

    void deleteByTenantId(TenantId tenantId);

    boolean deleteByTenantIdAndId(TenantId tenantId, IotHubInstalledItemId installedItemId);

}
