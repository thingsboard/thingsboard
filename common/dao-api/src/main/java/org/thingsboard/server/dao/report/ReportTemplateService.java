// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.report;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface ReportTemplateService extends EntityDaoService {

    ReportTemplate findReportTemplateById(TenantId tenantId, ReportTemplateId reportTemplateId);

    ReportTemplateInfo findReportTemplateInfoById(TenantId tenantId, ReportTemplateId reportTemplateId);

    ReportTemplate saveReportTemplate(ReportTemplate reportTemplate);

    ReportTemplate saveReportTemplate(ReportTemplate reportTemplate, boolean doValidate);

    void deleteReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId);

    List<ReportTemplateInfo> findReportTemplateInfoByIds(TenantId tenantId, List<ReportTemplateId> reportTemplateIds);

    PageData<ReportTemplateInfo> findReportTemplates(TenantId tenantId, ReportTemplateQuery query);

    PageData<ReportTemplateInfo> findCustomerReportTemplates(TenantId tenantId, CustomerId customerId, ReportTemplateQuery query);

    void deleteReportTemplatesByTenantId(TenantId tenantId);

    void deleteReportTemplatesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

}
