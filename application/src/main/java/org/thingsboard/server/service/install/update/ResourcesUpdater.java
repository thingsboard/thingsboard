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
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResourcesUpdater {
    private final ImageService imageService;
    private final ResourceService resourceService;
    private final WidgetsBundleDao widgetsBundleDao;
    private final WidgetTypeDao widgetTypeDao;
    private final WidgetTypeService widgetTypeService;
    private final TenantDao tenantDao;
    private final DashboardDao dashboardDao;
    private final DashboardService dashboardService;
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
        updateImages(widgetTypesIds, "widget type", imageService::updateImagesUsage, widgetTypeDao);
    }

    public void updateDashboardsImages() {
        log.info("Updating dashboards images...");
        updateImages("dashboard", dashboardDao::findIdsByTenantId, imageService::updateImagesUsage, dashboardDao);
    }

    public void createSystemImagesAndResources(Dashboard defaultDashboard) {
        defaultDashboard.setTenantId(TenantId.SYS_TENANT_ID);
        if (CollectionUtils.isNotEmpty(defaultDashboard.getResources())) {
            resourceService.importResources(defaultDashboard.getTenantId(), defaultDashboard.getResources());
        }
        imageService.updateImagesUsage(defaultDashboard);
        log.debug("Created/updated system images and resources for default dashboard '{}'", defaultDashboard.getTitle());
    }

    @SneakyThrows
    public void updateDashboardsResources() {
        log.info("Updating resources usage in dashboards");
        var executor = ThingsBoardExecutors.newLimitedTasksExecutor(4, 4, "dashboards-resources-upgrade");

        var dashboards = new PageDataIterable<>(dashboardService::findAllDashboardsIds, 512);
        AtomicInteger totalCount = new AtomicInteger();
        AtomicInteger updatedCount = new AtomicInteger();
        for (DashboardId dashboardId : dashboards) {
            executor.submit(() -> {
                Dashboard dashboard = dashboardService.findDashboardById(TenantId.SYS_TENANT_ID, dashboardId);
                boolean updated = resourceService.updateResourcesUsage(dashboard.getTenantId(), dashboard); // will convert resources ids to new structure
                if (updated) {
                    dashboardService.saveDashboard(dashboard);
                    updatedCount.incrementAndGet();
                }
                if (totalCount.incrementAndGet() % 1000 == 0) {
                    log.info("Processed {} dashboards, updated {}", totalCount, updatedCount);
                }
            });
        }

        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.HOURS)) {
            throw new RuntimeException("Dashboards resources update timeout"); // just in case, should happen
        }
        log.info("Updated {} dashboards", updatedCount);
    }

    @SneakyThrows
    public void updateWidgetsResources() {
        log.info("Updating resources usage in widgets");
        var executor = ThingsBoardExecutors.newLimitedTasksExecutor(4, 4, "widgets-resources-upgrade");

        AtomicInteger totalCount = new AtomicInteger();
        AtomicInteger updatedCount = new AtomicInteger();
        var widgets = new PageDataIterable<>(widgetTypeService::findAllWidgetTypesIds, 512);
        for (WidgetTypeId widgetTypeId : widgets) {
            executor.submit(() -> {
                WidgetTypeDetails widgetTypeDetails = widgetTypeService.findWidgetTypeDetailsById(TenantId.SYS_TENANT_ID, widgetTypeId);
                boolean updated = resourceService.updateResourcesUsage(widgetTypeDetails.getTenantId(), widgetTypeDetails);
                if (updated) {
                    widgetTypeService.saveWidgetType(widgetTypeDetails);
                    updatedCount.incrementAndGet();
                }
                if (totalCount.incrementAndGet() % 200 == 0) {
                    log.info("Processed {} widgets, updated {}", totalCount, updatedCount);
                }
            });
        }

        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.HOURS)) {
            throw new RuntimeException("Widgets resources update timeout");
        }
        log.info("Updated {} widgets", updatedCount);
    }

    public void updateDeviceProfilesImages() {
        log.info("Updating device profiles images...");
        var deviceProfiles = new PageDataIterable<>(deviceProfileDao::findAllWithImages, 256);
        updateImages(deviceProfiles, "device profile", imageService::replaceBase64WithImageUrl, deviceProfileDao);
    }

    public void updateAssetProfilesImages() {
        log.info("Updating asset profiles images...");
        var assetProfiles = new PageDataIterable<>(assetProfileDao::findAllWithImages, 256);
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
        int totalCount = 0;
        int updatedCount = 0;
        var counts = updateImages(entitiesIds, type, updater, dao, totalCount, updatedCount);
        totalCount = counts[0];
        updatedCount = counts[1];
        log.info("Updated {} {}s out of {}", updatedCount, type, totalCount);
    }

    private <E extends HasImage> void updateImages(String type, BiFunction<TenantId, PageLink, PageData<? extends EntityId>> entityIdsByTenantId,
                                                   Function<E, Boolean> updater, Dao<E> dao) {
        int tenantCount = 0;
        int totalCount = 0;
        int updatedCount = 0;
        var tenantIds = new PageDataIterable<>(tenantDao::findTenantsIds, 128);
        for (var tenantId : tenantIds) {
            tenantCount++;
            var entitiesIds = new PageDataIterable<>(link -> entityIdsByTenantId.apply(tenantId, link), 128);
            var counts = updateImages(entitiesIds, type, updater, dao, totalCount, updatedCount);
            totalCount = counts[0];
            updatedCount = counts[1];
            if (tenantCount % 100 == 0) {
                log.info("Update {}s images: processed {} tenants so far", type, tenantCount);
            }
        }
        log.info("Updated {} {}s out of {}", updatedCount, type, totalCount);
    }

    private <E extends HasImage> int[] updateImages(Iterable<? extends EntityId> entitiesIds, String type,
                                                    Function<E, Boolean> updater, Dao<E> dao, int totalCount, int updatedCount) {
        for (EntityId id : entitiesIds) {
            totalCount++;
            E entity;
            try {
                entity = dao.findById(TenantId.SYS_TENANT_ID, id.getId());
            } catch (Exception e) {
                log.error("Failed to update {} images: error fetching entity by id [{}]: {}", type, id.getId(), StringUtils.abbreviate(e.toString(), 1000));
                continue;
            }
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
        return new int[]{totalCount, updatedCount};
    }

}
