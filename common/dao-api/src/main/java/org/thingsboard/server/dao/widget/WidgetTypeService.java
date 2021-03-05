/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;

import java.util.List;

public interface WidgetTypeService {

    WidgetType findWidgetTypeById(TenantId tenantId, WidgetTypeId widgetTypeId);

    WidgetTypeDetails findWidgetTypeDetailsById(TenantId tenantId, WidgetTypeId widgetTypeId);

    WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetType);

    void deleteWidgetType(TenantId tenantId, WidgetTypeId widgetTypeId);

    List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias);

    List<WidgetTypeDetails> findWidgetTypesDetailsByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias);

    List<WidgetTypeInfo> findWidgetTypesInfosByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias);

    WidgetType findWidgetTypeByTenantIdBundleAliasAndAlias(TenantId tenantId, String bundleAlias, String alias);

    void deleteWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias);

}
