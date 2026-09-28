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
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface ReportService extends EntityDaoService {

    Report createReport(Report report, byte[] data);

    Report findReportById(TenantId tenantId, ReportId reportId);

    ReportData getReportDataById(TenantId tenantId, ReportId reportId);

    ReportData getReportDataByPublicKey(String publicKey);

    Report updateReport(Report report);

    void deleteReport(TenantId tenantId, ReportId reportId);

    PageData<Report> findReports(TenantId tenantId, CustomerId customerId, boolean includeCustomers, PageLink pageLink);

    PageData<ReportInfo> findReportInfos(TenantId tenantId, CustomerId customerId, ReportInfoQuery query);

    void deleteReportsByTenantId(TenantId tenantId);

    void deleteReportsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    List<ReportInfo> findReportInfoByIds(TenantId tenantId, List<ReportId> reportIds);
}
