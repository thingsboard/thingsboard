// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.dao.report.ReportTemplateService;

@AllArgsConstructor
@Slf4j
public class ReportTemplateEdgeEventFetcher extends BasePageableEdgeEventFetcher<ReportTemplateInfo> {

    private final ReportTemplateService reportTemplateService;
    private final CustomerId customerId;

    @Override
    PageData<ReportTemplateInfo> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        ReportTemplateQuery query = new ReportTemplateQuery(pageLink, false, null, null);
        if (customerId == null) {
            return reportTemplateService.findReportTemplates(tenantId, query);
        } else {
            return reportTemplateService.findCustomerReportTemplates(tenantId, customerId, query);
        }
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, ReportTemplateInfo reportTemplateInfo) {
        return EdgeUtils.constructEdgeEvent(
                tenantId,
                edge.getId(),
                EdgeEventType.REPORT_TEMPLATE,
                EdgeEventActionType.ADDED,
                reportTemplateInfo.getId(),
                null
        );
    }

}
