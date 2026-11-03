// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.widget;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@Data
@Builder
public class WidgetTypeFilter {

    private TenantId tenantId;
    private boolean fullSearch;
    private boolean scadaFirst;
    DeprecatedFilter deprecatedFilter;
    List<String> widgetTypes;

}
