// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiUsageLimitNotificationInfo implements RuleOriginatedNotificationInfo {

    private ApiFeature feature;
    private ApiUsageRecordKey recordKey;
    private ApiUsageStateValue status;
    private String limit;
    private String currentValue;
    private TenantId tenantId;
    private String tenantName;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "feature", feature.getLabel(),
                "unitLabel", recordKey.getUnitLabel(),
                "status", status.name().toLowerCase(),
                "limit", limit,
                "currentValue", currentValue,
                "tenantId", tenantId.toString(),
                "tenantName", tenantName
        );
    }

    @Override
    public TenantId getAffectedTenantId() {
        return tenantId;
    }

}
