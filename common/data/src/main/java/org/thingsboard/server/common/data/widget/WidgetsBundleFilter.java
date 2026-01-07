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
package org.thingsboard.server.common.data.widget;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
public class WidgetsBundleFilter {

    private TenantId tenantId;
    private boolean fullSearch;
    private boolean scadaFirst;

    public static WidgetsBundleFilter fromTenantId(TenantId tenantId) {
        return WidgetsBundleFilter.builder().tenantId(tenantId).fullSearch(false).scadaFirst(false).build();
    }

    public static WidgetsBundleFilter fullSearchFromTenantId(TenantId tenantId) {
        return WidgetsBundleFilter.builder().tenantId(tenantId).fullSearch(true).scadaFirst(false).build();
    }

}
