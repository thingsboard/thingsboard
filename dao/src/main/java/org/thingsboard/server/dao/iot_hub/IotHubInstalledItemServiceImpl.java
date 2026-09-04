// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.iot_hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.model.sql.IotHubInstalledItemEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@RequiredArgsConstructor
@Slf4j
class IotHubInstalledItemServiceImpl implements IotHubInstalledItemService {

    private final IotHubInstalledItemDao iotHubInstalledItemDao;

    @Override
    public IotHubInstalledItem save(TenantId tenantId, IotHubInstalledItem item) {
        log.debug("[{}] Saving IoT Hub installed item: {}", tenantId, item);
        return iotHubInstalledItemDao.save(tenantId, item);
    }

    @Override
    public IotHubInstalledItem findById(TenantId tenantId, IotHubInstalledItemId id) {
        return iotHubInstalledItemDao.findByTenantIdAndId(tenantId, id).orElse(null);
    }

    @Override
    public PageData<IotHubInstalledItem> findByTenantId(TenantId tenantId, List<String> itemTypes, UUID itemId, PageLink pageLink) {
        validatePageLink(pageLink, IotHubInstalledItemEntity.ALLOWED_SORT_PROPERTIES);
        return iotHubInstalledItemDao.findByTenantId(tenantId, itemTypes, itemId, pageLink);
    }

    @Override
    public List<UUID> findInstalledItemIdsByTenantId(TenantId tenantId) {
        return iotHubInstalledItemDao.findInstalledItemIdsByTenantId(tenantId);
    }

    @Override
    public List<UUID> findInstalledItemIdsByTenantIdAndItemIdIn(TenantId tenantId, Collection<UUID> itemIds) {
        return iotHubInstalledItemDao.findInstalledItemIdsByTenantIdAndItemIdIn(tenantId, itemIds);
    }

    @Override
    public long countByTenantId(TenantId tenantId, String itemType) {
        return iotHubInstalledItemDao.countByTenantId(tenantId, itemType);
    }

    @Override
    public Map<UUID, Long> findInstalledItemCounts(TenantId tenantId, String itemType) {
        return iotHubInstalledItemDao.findInstalledItemCounts(tenantId, itemType);
    }

    @Override
    public void deleteById(TenantId tenantId, IotHubInstalledItemId id) {
        iotHubInstalledItemDao.deleteByTenantIdAndId(tenantId, id);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        iotHubInstalledItemDao.deleteByTenantId(tenantId);
    }

}
