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
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.function.BiFunction;
import java.util.function.Function;

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
        updateImages(widgetsBundles, "bundle", imageService::replaceBase64WithImageUrl, widgetsBundleDao);
    }

    public void updateWidgetTypesImages() {
        log.info("Updating widget types images...");
        var widgetTypesIds = new PageDataIterable<>(widgetTypeDao::findAllWidgetTypesIds, 1024);
        updateImages(widgetTypesIds, "widget type", imageService::replaceBase64WithImageUrl, widgetTypeDao);
    }

    public void updateDashboardsImages() {
        log.info("Updating dashboards images...");
        var dashboardsIds = new PageDataIterable<>(dashboardDao::findAllIds, 1024);
        updateImages(dashboardsIds, "dashboard", imageService::replaceBase64WithImageUrl, dashboardDao);
    }

    public void createSystemImages(Dashboard defaultDashboard) {
        defaultDashboard.setTenantId(TenantId.SYS_TENANT_ID);
        boolean created = imageService.replaceBase64WithImageUrl(defaultDashboard);
        if (created) {
            log.debug("Created system images for default dashboard '{}'", defaultDashboard.getTitle());
        }
    }

    public void updateDeviceProfilesImages() {
        log.info("Updating device profiles images...");
        var deviceProfiles = new PageDataIterable<>(deviceProfileDao::findAll, 256);
        updateImages(deviceProfiles, "device profile", imageService::replaceBase64WithImageUrl, deviceProfileDao);
    }

    public void updateAssetProfilesImages() {
        log.info("Updating asset profiles images...");
        var assetProfiles = new PageDataIterable<>(assetProfileDao::findAll, 256);
        updateImages(assetProfiles, "asset profile", imageService::replaceBase64WithImageUrl, assetProfileDao);
    }

    private <E extends HasImage> void updateImages(Iterable<E> entities, String type,
                                                   BiFunction<E, String, Boolean> updater, Dao<E> dao) {
        int updatedCount = 0;
        int totalCount = 0;
        for (E entity : entities) {
            totalCount++;
            try {
                boolean updated = updater.apply(entity, type);
                if (updated) {
                    dao.save(entity.getTenantId(), entity);
                    log.debug("[{}][{}] Updated {} images", entity.getTenantId(), entity.getName(), type);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}] Failed to update {} images", entity.getTenantId(), entity.getName(), type, e);
            }
            if (totalCount % 100 == 0) {
                log.info("Processed {} {}s so far", totalCount, type);
            }
        }
        log.info("Updated {} {}s out of {}", updatedCount, type, totalCount);
    }

    private <E extends HasImage> void updateImages(Iterable<? extends EntityId> entitiesIds, String type,
                                                   Function<E, Boolean> updater, Dao<E> dao) {
        int updatedCount = 0;
        int totalCount = 0;
        for (EntityId id : entitiesIds) {
            totalCount++;
            E entity = dao.findById(TenantId.SYS_TENANT_ID, id.getId());
            try {
                boolean updated = updater.apply(entity);
                if (updated) {
                    dao.save(entity.getTenantId(), entity);
                    log.debug("[{}][{}] Updated {} images", entity.getTenantId(), entity.getName(), type);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}] Failed to update {} images", entity.getTenantId(), entity.getName(), type, e);
            }
            if (totalCount % 100 == 0) {
                log.info("Processed {} {}s so far", totalCount, type);
            }
        }
        log.info("Updated {} {}s out of {}", updatedCount, type, totalCount);
    }

}
