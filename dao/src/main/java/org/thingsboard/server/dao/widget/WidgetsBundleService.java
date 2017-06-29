/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.widget;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.util.List;

public interface WidgetsBundleService {

    WidgetsBundle findWidgetsBundleById(WidgetsBundleId widgetsBundleId);

    WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle);

    void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId);

    WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias);

    TextPageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(TextPageLink pageLink);

    List<WidgetsBundle> findSystemWidgetsBundles();

    TextPageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink);

    List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId);

    void deleteWidgetsBundlesByTenantId(TenantId tenantId);

}
