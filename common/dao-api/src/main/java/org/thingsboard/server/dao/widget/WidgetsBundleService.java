// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.widget;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleFilter;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.stream.Stream;

public interface WidgetsBundleService extends EntityDaoService {

    WidgetsBundle findWidgetsBundleById(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle);

    void deleteWidgetsBundle(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias);

    PageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(WidgetsBundleFilter widgetsBundleFilter, PageLink pageLink);

    List<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId);

    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter widgetsBundleFilter, PageLink pageLink);

    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantIdAndPageLink(WidgetsBundleFilter widgetsBundleFilter, PageLink pageLink);

    List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId);

    ListenableFuture<List<WidgetsBundle>> findSystemWidgetsBundlesByIdsAsync(TenantId tenantId, List<WidgetsBundleId> widgetsBundleIds);

    ListenableFuture<List<WidgetsBundle>> findAllTenantWidgetsBundlesByIdsAsync(TenantId tenantId, List<WidgetsBundleId> widgetsBundleIds);

    List<WidgetsBundle> findSystemOrTenantWidgetsBundlesByIds(TenantId tenantId, List<WidgetsBundleId> widgetsBundleIds);

    void deleteWidgetsBundlesByTenantId(TenantId tenantId);

    void updateSystemWidgets(Stream<String> bundles, Stream<String> widgets);

}
