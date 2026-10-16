// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
