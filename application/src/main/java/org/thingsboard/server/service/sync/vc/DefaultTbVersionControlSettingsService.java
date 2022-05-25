/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.VersionControlAuthMethod;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbVersionControlSettingsService implements TbVersionControlSettingsService {

    public static final String SETTINGS_KEY = "entitiesVersionControl";
    private final AdminSettingsService adminSettingsService;
    private final TbTransactionalCache<TenantId, EntitiesVersionControlSettings> cache;

    @Override
    public EntitiesVersionControlSettings restore(TenantId tenantId, EntitiesVersionControlSettings settings) {
        EntitiesVersionControlSettings storedSettings = get(tenantId);
        if (storedSettings != null) {
            VersionControlAuthMethod authMethod = settings.getAuthMethod();
            if (VersionControlAuthMethod.USERNAME_PASSWORD.equals(authMethod) && settings.getPassword() == null) {
                settings.setPassword(storedSettings.getPassword());
            } else if (VersionControlAuthMethod.PRIVATE_KEY.equals(authMethod) && settings.getPrivateKey() == null) {
                settings.setPrivateKey(storedSettings.getPrivateKey());
                if (settings.getPrivateKeyPassword() == null) {
                    settings.setPrivateKeyPassword(storedSettings.getPrivateKeyPassword());
                }
            }
        }
        return settings;
    }

    @Override
    public EntitiesVersionControlSettings get(TenantId tenantId) {
        return cache.getAndPutInTransaction(tenantId, () -> {
            AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, SETTINGS_KEY);
            if (adminSettings != null) {
                try {
                    return JacksonUtil.convertValue(adminSettings.getJsonValue(), EntitiesVersionControlSettings.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load version control settings!", e);
                }
            }
            return null;
        }, true);
    }

    @Override
    public EntitiesVersionControlSettings save(TenantId tenantId, EntitiesVersionControlSettings versionControlSettings) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(tenantId, SETTINGS_KEY);
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setKey(SETTINGS_KEY);
            adminSettings.setTenantId(tenantId);
        }
        adminSettings.setJsonValue(JacksonUtil.valueToTree(versionControlSettings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        EntitiesVersionControlSettings savedVersionControlSettings;
        try {
            savedVersionControlSettings = JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), EntitiesVersionControlSettings.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load version control settings!", e);
        }
        //API calls to adminSettingsService are not in transaction, so we can simply evict the cache.
        cache.evict(tenantId);
        return savedVersionControlSettings;
    }

    @Override
    public boolean delete(TenantId tenantId) {
        boolean result = adminSettingsService.deleteAdminSettings(tenantId, SETTINGS_KEY);
        cache.evict(tenantId);
        return result;
    }

}
