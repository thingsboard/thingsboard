/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.mobile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.exception.DataValidationException;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseMobileAppSettingsService extends AbstractCachedService<TenantId, MobileAppSettings, MobileAppSettingsEvictEvent> implements MobileAppSettingsService {

    @Value("${cache.specs.qrSecretKey.timeToLiveInMinutes:2}")
    private int secretKeyTtl;

    private static final int MIN_TIME_TO_DOWNLOAD_MOBILE_APP_IN_MIN = 1;
    private final MobileAppSettingsDao mobileAppSettingsDao;

    @Override
    public MobileAppSettings saveMobileAppSettings(TenantId tenantId, MobileAppSettings settings) {
        if (settings.getSettings() != null && settings.getSettings().get("qrSecretKeyRefreshRateInMin") != null
                && (settings.getSettings().get("qrSecretKeyRefreshRateInMin").asInt() + MIN_TIME_TO_DOWNLOAD_MOBILE_APP_IN_MIN > secretKeyTtl)) {
            throw new DataValidationException("Refresh rate should be less than server secret key ttl");
        }
        MobileAppSettings mobileAppSettings = mobileAppSettingsDao.save(tenantId, settings);
        publishEvictEvent(new MobileAppSettingsEvictEvent(tenantId));
        return mobileAppSettings;
    }

    @Override
    public MobileAppSettings getMobileAppSettings(TenantId tenantId) {
        log.trace("Executing getMobileAppSettings for tenant [{}] ", tenantId);
        MobileAppSettings mobileAppSettings = cache.getAndPutInTransaction(tenantId,
                () -> mobileAppSettingsDao.findByTenantId(tenantId), false);
        return constructMobileAppSettings(mobileAppSettings);
    }

    @TransactionalEventListener(classes = MobileAppSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(MobileAppSettingsEvictEvent event) {
        cache.evict(event.getTenantId());
    }

    private MobileAppSettings constructMobileAppSettings(MobileAppSettings mobileAppSettings) {
        if (mobileAppSettings == null) {
            mobileAppSettings = new MobileAppSettings();

            ObjectNode settings = JacksonUtil.newObjectNode();
            settings.put("useDefault", true);
            settings.put("showOnHomePage", true);
            settings.put("qrLabel", "Scan to connect or download mobile app");
            settings.put("qrSecretKeyRefreshRateInMin", 1);

            ObjectNode androidSettings = JacksonUtil.newObjectNode();
            androidSettings.put("badge", "original");
            androidSettings.put("badgePosition", "Right");
            settings.set("android", androidSettings);

            ObjectNode iOSSettings = JacksonUtil.newObjectNode();
            iOSSettings.put("badge", "original");
            iOSSettings.put("badgePosition", "Right");
            settings.set("iOS", iOSSettings);

            mobileAppSettings.setSettings(settings);
        }
        return mobileAppSettings;
    }


}
