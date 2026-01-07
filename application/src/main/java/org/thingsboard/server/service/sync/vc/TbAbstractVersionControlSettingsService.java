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
package org.thingsboard.server.service.sync.vc;

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.io.Serializable;

public abstract class TbAbstractVersionControlSettingsService<T extends Serializable> {

    private final String settingsKey;
    private final AdminSettingsService adminSettingsService;
    private final TbTransactionalCache<TenantId, T> cache;
    private final Class<T> clazz;

    public TbAbstractVersionControlSettingsService(AdminSettingsService adminSettingsService, TbTransactionalCache<TenantId, T> cache, Class<T> clazz, String settingsKey) {
        this.adminSettingsService = adminSettingsService;
        this.cache = cache;
        this.clazz = clazz;
        this.settingsKey = settingsKey;
    }

    public T get(TenantId tenantId) {
        return cache.getAndPutInTransaction(tenantId, () -> {
            AdminSettings adminSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, settingsKey);
            if (adminSettings != null) {
                try {
                    return JacksonUtil.convertValue(adminSettings.getJsonValue(), clazz);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load " + settingsKey + " settings!", e);
                }
            }
            return null;
        }, true);
    }

    public T save(TenantId tenantId, T settings) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, settingsKey);
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setKey(settingsKey);
            adminSettings.setTenantId(tenantId);
        }
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        T savedSettings;
        try {
            savedSettings = JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load auto commit settings!", e);
        }
        //API calls to adminSettingsService are not in transaction, so we can simply evict the cache.
        cache.evict(tenantId);
        return savedSettings;
    }

    public boolean delete(TenantId tenantId) {
        boolean result = adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, settingsKey);
        cache.evict(tenantId);
        return result;
    }

}
