// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;

import java.util.function.Consumer;

public interface DashboardReportService {

    void generateDashboardReport(String baseUrl, DashboardId dashboardId, TenantId tenantId, UserId userId, String reportName,
                                 JsonNode reportParams, String accessToken, long accessTokenExpiration,
                                 Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure);

    void generateReport(TenantId tenantId, DashboardReportConfig reportConfig,
                        String reportsServerEndpointUrl,
                        Consumer<DashboardReportData> onSuccess,
                        Consumer<Throwable> onFailure) throws ThingsboardException;

}
