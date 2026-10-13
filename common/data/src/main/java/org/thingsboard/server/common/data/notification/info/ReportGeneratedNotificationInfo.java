// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.TbReportFormat;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportGeneratedNotificationInfo implements RuleOriginatedNotificationInfo {

    private TenantId tenantId;
    private CustomerId customerId;
    private TbReportFormat reportFormat;
    private String reportName;
    private UserId userId;
    private String reportPublicUrl;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "reportFormat", reportFormat.name(),
                "reportName", reportName,
                "reportPublicUrl", reportPublicUrl != null ? reportPublicUrl : ""
        );
    }

    @Override
    public TenantId getAffectedTenantId() {
        return tenantId;
    }

    @Override
    public CustomerId getAffectedCustomerId() {
        return customerId;
    }

    @Override
    public UserId getAffectedUserId() {
        return userId;
    }

}
