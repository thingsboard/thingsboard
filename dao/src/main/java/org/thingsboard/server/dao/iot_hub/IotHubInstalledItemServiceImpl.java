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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        return iotHubInstalledItemDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<IotHubInstalledItem> findByTenantId(TenantId tenantId, List<String> itemTypes, UUID itemId, PageLink pageLink) {
        return iotHubInstalledItemDao.findByTenantId(tenantId, itemTypes, itemId, pageLink);
    }

    @Override
    public List<UUID> findInstalledItemIdsByTenantId(TenantId tenantId) {
        return iotHubInstalledItemDao.findInstalledItemIdsByTenantId(tenantId);
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
        iotHubInstalledItemDao.removeById(tenantId, id.getId());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        iotHubInstalledItemDao.deleteByTenantId(tenantId);
    }

}
