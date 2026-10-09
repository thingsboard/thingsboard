// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.report;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface ReportTemplateInfoDao extends Dao<ReportTemplateInfo> {

    PageData<ReportTemplateInfo> findReportTemplates(UUID tenantId, ReportTemplateQuery query);

    PageData<ReportTemplateInfo> findCustomerReportTemplates(UUID tenantId, UUID customerId, ReportTemplateQuery query);

    List<ReportTemplateInfo> findReportTemplatesByIds(UUID tenantId, List<UUID> reportTemplateIds);

}
