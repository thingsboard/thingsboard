// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbReportTemplateService extends AbstractTbEntityService implements TbReportTemplateService {

    private final ReportTemplateService reportTemplateService;

    @Override
    public ReportTemplate save(ReportTemplate reportTemplate, User user) throws Exception {
        try {
            ReportTemplate savedReportTemplate = checkNotNull(reportTemplateService.saveReportTemplate(reportTemplate));
            autoCommit(user, savedReportTemplate.getId());
            logEntityActionService.logEntityAction(user.getTenantId(), savedReportTemplate.getId(), savedReportTemplate,
                    savedReportTemplate.getCustomerId(),
                    reportTemplate.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user);
            return savedReportTemplate;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.REPORT_TEMPLATE), reportTemplate,
                    reportTemplate.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public void delete(ReportTemplate reportTemplate, User user) {
        ActionType actionType = ActionType.DELETED;
        ReportTemplateId reportTemplateId = reportTemplate.getId();
        try {
            reportTemplateService.deleteReportTemplate(user.getTenantId(), reportTemplateId);
            logEntityActionService.logEntityAction(user.getTenantId(), reportTemplateId, reportTemplate,
                    reportTemplate.getCustomerId(), actionType, user, reportTemplateId.getId().toString());

        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.REPORT_TEMPLATE),
                    actionType, user, e, reportTemplateId.getId().toString());
            throw e;
        }
    }
}
