// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.report;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportInfoQuery;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReportDao extends Dao<Report> {

    void saveData(TenantId tenantId, ReportId reportId, byte[] data);

    ReportData getReportDataById(TenantId tenantId, ReportId reportId);

    PageData<Report> findReports(TenantId tenantId, CustomerId customerId, boolean includeCustomers, PageLink pageLink);

    PageData<ReportInfo> findReportInfos(TenantId tenantId, CustomerId customerId, ReportInfoQuery query);

    List<ReportInfo> findReportByIds(TenantId tenantId, List<UUID> toUUIDs);

    ReportData getReportDataByPublicKey(String publicKey);

    void deleteByTenantId(TenantId tenantId);

    void deleteByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    Map<String, Long> countReportsByType();

}
