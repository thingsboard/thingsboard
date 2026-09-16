// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntitiesLimitNotificationInfo implements RuleOriginatedNotificationInfo {

    private EntityType entityType;
    private long currentCount;
    private long limit;
    private int percents;
    private TenantId tenantId;
    private String tenantName;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "entityType", entityType.getNormalName(),
                "currentCount", String.valueOf(currentCount),
                "limit", String.valueOf(limit),
                "percents", String.valueOf(percents),
                "tenantId", tenantId.toString(),
                "tenantName", tenantName
        );
    }

    @Override
    public TenantId getAffectedTenantId() {
        return tenantId;
    }

}
