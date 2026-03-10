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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Optional;
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
    public Optional<IotHubInstalledItem> findByTenantIdAndItemId(TenantId tenantId, UUID itemId) {
        return iotHubInstalledItemDao.findByTenantIdAndItemId(tenantId, itemId);
    }

    @Override
    public PageData<IotHubInstalledItem> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return iotHubInstalledItemDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public List<IotHubInstalledItemInfo> findInstalledItemInfosByTenantId(TenantId tenantId) {
        return iotHubInstalledItemDao.findInstalledItemInfosByTenantId(tenantId);
    }

    @Override
    public boolean deleteByTenantIdAndItemId(TenantId tenantId, UUID itemId) {
        return iotHubInstalledItemDao.deleteByTenantIdAndItemId(tenantId, itemId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        iotHubInstalledItemDao.deleteByTenantId(tenantId);
    }

}
