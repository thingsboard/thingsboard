// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseReportTemplateProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<ReportTemplate> reportTemplateDataValidator;

    protected Boolean saveOrUpdateReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId, ReportTemplateUpdateMsg reportTemplateUpdateMsg) {
        boolean created = false;
        try {
            ReportTemplate reportTemplate = JacksonUtil.fromString(reportTemplateUpdateMsg.getEntity(), ReportTemplate.class, true);
            if (reportTemplate == null) {
                throw new RuntimeException("[{" + tenantId + "}] reportTemplateUpdateMsg {" + reportTemplateUpdateMsg + "} cannot be converted to report template");
            }
            ReportTemplate reportTemplateById = edgeCtx.getReportTemplateService().findReportTemplateById(tenantId, reportTemplateId);
            if (reportTemplateById == null) {
                created = true;
                reportTemplate.setId(null);
            }
            reportTemplateDataValidator.validate(reportTemplate, ReportTemplate::getTenantId);
            if (created) {
                reportTemplate.setId(reportTemplateId);
            }
            edgeCtx.getReportTemplateService().saveReportTemplate(reportTemplate, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process report template update msg [{}]", tenantId, reportTemplateUpdateMsg, e);
            throw e;
        }
        return created;
    }

    protected void deleteReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId) {
        ReportTemplate reportTemplateToDelete = edgeCtx.getReportTemplateService().findReportTemplateById(tenantId, reportTemplateId);
        if (reportTemplateToDelete != null) {
            edgeCtx.getReportTemplateService().deleteReportTemplate(tenantId, reportTemplateId);
            pushReportTemplateDeletedEventToRuleEngine(tenantId, reportTemplateToDelete);
        }
    }

    protected void pushReportTemplateDeletedEventToRuleEngine(TenantId tenantId, ReportTemplate reportTemplate) {
        pushReportTemplateEventToRuleEngine(tenantId, reportTemplate, TbMsgType.ENTITY_DELETED);
    }

    protected void pushReportTemplateEventToRuleEngine(TenantId tenantId, ReportTemplate reportTemplate, TbMsgType msgType) {
        pushReportTemplateEventToRuleEngine(tenantId, null, reportTemplate, msgType);
    }

    protected void pushReportTemplateEventToRuleEngine(TenantId tenantId, Edge edge, ReportTemplate reportTemplate, TbMsgType msgType) {
        try {
            CustomerId customerId = reportTemplate.getCustomerId();
            String reportTemplateAsString = JacksonUtil.toString(reportTemplate);
            TbMsgMetaData tbMsgMetaData = edge == null ? new TbMsgMetaData() : getEdgeActionTbMsgMetaData(edge, customerId);
            pushEntityEventToRuleEngine(tenantId, reportTemplate.getId(), customerId, msgType, reportTemplateAsString, tbMsgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push report template action to rule engine: {}", tenantId, reportTemplate.getId(), msgType.name(), e);
        }
    }
}
