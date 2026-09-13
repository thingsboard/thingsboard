// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.trendz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultTrendzSettingsService implements TrendzSettingsService {

    private final AdminSettingsService adminSettingsService;

    private static final String SETTINGS_KEY = "trendz";

    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public void saveTrendzSettings(TenantId tenantId, TrendzSettings settings) {
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setTenantId(tenantId);
                    newAdminSettings.setKey(SETTINGS_KEY);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(tenantId, adminSettings);
    }

    @Cacheable(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public TrendzSettings findTrendzSettings(TenantId tenantId) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), TrendzSettings.class))
                .orElseGet(TrendzSettings::new);
    }

    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public void deleteTrendzSettings(TenantId tenantId) {
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY);
    }

}
