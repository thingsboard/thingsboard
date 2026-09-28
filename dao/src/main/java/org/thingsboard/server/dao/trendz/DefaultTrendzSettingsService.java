// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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

    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'system'")
    @Override
    public void saveTrendzSettings(TrendzSettings settings) {
        log.trace("Executing saveTrendzSettings [{}]", settings);
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setTenantId(TenantId.SYS_TENANT_ID);
                    newAdminSettings.setKey(SETTINGS_KEY);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings);
    }

    @Cacheable(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'system'")
    @Override
    public TrendzSettings findTrendzSettings() {
        log.trace("Executing findTrendzSettings");
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), TrendzSettings.class))
                .orElse(null);
    }

    @CacheEvict(cacheNames = CacheConstants.TRENDZ_SETTINGS_CACHE, key = "'system'")
    @Override
    public void deleteTrendzSettings() {
        log.trace("Executing deleteTrendzSettings");
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY);
    }

}
