/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImagesUpdater {
    private final ImageService imageService;
    private final WidgetsBundleDao widgetsBundleDao;
    private final WidgetTypeDao widgetTypeDao;
    private final DashboardDao dashboardDao;
    private final DeviceProfileDao deviceProfileDao;
    private final AssetProfileDao assetProfileDao;

    public void updateWidgetsBundlesImages() {
        log.info("Updating widgets bundles images...");
        var widgetsBundles = new PageDataIterable<>(widgetsBundleDao::findAllWidgetsBundles, 128);
        int updatedCount = 0;
        int totalCount = 0;
        for (WidgetsBundle widgetsBundle : widgetsBundles) {
            totalCount++;
            try {
                boolean updated = imageService.replaceBase64WithImageUrl(widgetsBundle, "bundle");
                if (updated) {
                    widgetsBundleDao.save(widgetsBundle.getTenantId(), widgetsBundle);
                    log.debug("[{}][{}][{}] Updated widgets bundle images", widgetsBundle.getTenantId(), widgetsBundle.getId(), widgetsBundle.getTitle());
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}][{}] Failed to update widgets bundle images", widgetsBundle.getTenantId(), widgetsBundle.getId(), widgetsBundle.getTitle(), e);
            }
        }
        log.info("Updated {} widgets bundles out of {}", updatedCount, totalCount);
    }

    public void updateWidgetTypesImages() {
        log.info("Updating widget types images...");
        var widgetTypes = new PageDataIterable<>(widgetTypeDao::findAllWidgetTypesIds, 1024);
        int updatedCount = 0;
        int totalCount = 0;
        for (WidgetTypeId widgetTypeId : widgetTypes) {
            totalCount++;
            WidgetTypeDetails widgetTypeDetails = widgetTypeDao.findById(TenantId.SYS_TENANT_ID, widgetTypeId.getId());
            try {
                boolean updated = imageService.replaceBase64WithImageUrl(widgetTypeDetails);
                if (updated) {
                    widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
                    log.debug("[{}][{}][{}] Updated widget type images", widgetTypeDetails.getTenantId(), widgetTypeDetails.getId(), widgetTypeDetails.getName());
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}][{}] Failed to update widget type images", widgetTypeDetails.getTenantId(), widgetTypeDetails.getId(), widgetTypeDetails.getName(), e);
            }
        }
        log.info("Updated {} widget types out of {}", updatedCount, totalCount);
    }

    public void updateDashboardsImages() {
        log.info("Updating dashboards images...");
        var dashboards = new PageDataIterable<>(dashboardDao::findAllIds, 1024);
        int updatedCount = 0;
        int totalCount = 0;
        for (DashboardId dashboardId : dashboards) {
            totalCount++;
            Dashboard dashboard = dashboardDao.findById(TenantId.SYS_TENANT_ID, dashboardId.getId());
            try {
                boolean updated = imageService.replaceBase64WithImageUrl(dashboard);
                if (updated) {
                    dashboardDao.save(dashboard.getTenantId(), dashboard);
                    log.info("[{}][{}][{}] Updated dashboard images", dashboard.getTenantId(), dashboardId, dashboard.getTitle());
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}][{}] Failed to update dashboard images", dashboard.getTenantId(), dashboardId, dashboard.getTitle(), e);
            }
        }
        log.info("Updated {} dashboards out of {}", updatedCount, totalCount);
    }

    public void updateDeviceProfilesImages() {
        log.info("Updating device profiles images...");
        var deviceProfiles = new PageDataIterable<>(deviceProfileDao::findAll, 256);
        int updatedCount = 0;
        int totalCount = 0;
        for (DeviceProfile deviceProfile : deviceProfiles) {
            totalCount++;
            try {
                boolean updated = imageService.replaceBase64WithImageUrl(deviceProfile, "device profile");
                if (updated) {
                    deviceProfileDao.save(deviceProfile.getTenantId(), deviceProfile);
                    log.debug("[{}][{}][{}] Updated device profile images", deviceProfile.getTenantId(), deviceProfile.getId(), deviceProfile.getName());
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}][{}] Failed to update device profile images", deviceProfile.getTenantId(), deviceProfile.getId(), deviceProfile.getName(), e);
            }
        }
        log.info("Updated {} device profiles out of {}", updatedCount, totalCount);
    }

    public void updateAssetProfilesImages() {
        log.info("Updating asset profiles images...");
        var assetProfiles = new PageDataIterable<>(assetProfileDao::findAll, 256);
        int updatedCount = 0;
        int totalCount = 0;
        for (AssetProfile assetProfile : assetProfiles) {
            totalCount++;
            try {
                boolean updated = imageService.replaceBase64WithImageUrl(assetProfile, "asset profile");
                if (updated) {
                    assetProfileDao.save(assetProfile.getTenantId(), assetProfile);
                    log.debug("[{}][{}][{}] Updated asset profile images", assetProfile.getTenantId(), assetProfile.getId(), assetProfile.getName());
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}][{}] Failed to update asset profile images", assetProfile.getTenantId(), assetProfile.getId(), assetProfile.getName(), e);
            }
        }
        log.info("Updated {} asset profiles out of {}", updatedCount, totalCount);
    }

}
